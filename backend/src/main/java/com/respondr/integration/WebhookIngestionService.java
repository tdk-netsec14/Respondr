package com.respondr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.auth.User;
import com.respondr.common.exception.ApiException;
import com.respondr.common.exception.ErrorCode;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.redis.RedisService;
import com.respondr.incident.*;
import com.respondr.oncall.OnCallService;
import com.respondr.organization.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final AlertIntegrationRepository integrationRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final OnCallService onCallService;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final com.respondr.event.OutboxService outboxService;

    public WebhookIngestionService(
            AlertIntegrationRepository integrationRepository,
            AlertRepository alertRepository,
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            OnCallService onCallService,
            ObjectMapper objectMapper,
            RedisService redisService,
            com.respondr.event.OutboxService outboxService) {
        this.integrationRepository = integrationRepository;
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.onCallService = onCallService;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.outboxService = outboxService;
    }

    public WebhookResponse processWebhook(String integrationKey, String signatureHeader, String rawPayload) {
        // Rate limiting check via Redis
        if (!redisService.tryAcquireRateLimit(integrationKey, 100, 60)) {
            log.warn("Rate limit exceeded for integration {}", integrationKey);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for integration: " + integrationKey);
        }

        AlertIntegration integration = integrationRepository.findByKey(integrationKey)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND, "Alert integration not found: " + integrationKey));

        if (!integration.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Alert integration is inactive");
        }

        // Verify HMAC signature
        if (signatureHeader != null && !signatureHeader.isBlank()) {
            if (!HmacUtils.verifyHmacSha256(rawPayload, integration.getSecretKey(), signatureHeader)) {
                log.warn("Invalid HMAC signature received for integration {}", integrationKey);
                throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN, "Invalid webhook signature");
            }
        }

        Organization org = integration.getOrganization();

        // Extract external ID and generate fingerprint
        NormalizedAlertData alertData = parsePayload(integration.getProvider(), rawPayload);
        String fingerprint = alertData.fingerprint();
        String dedupCacheKey = integration.getId() + ":" + fingerprint;

        // Redis Fast-path Dedup Check
        if (redisService.isAlertCached(dedupCacheKey)) {
            log.info("Duplicate alert caught in Redis dedup cache (fingerprint: {})", fingerprint);
            Optional<Alert> existingAlert = alertRepository.findByIntegrationIdAndFingerprint(integration.getId(), fingerprint);
            if (existingAlert.isPresent()) {
                Alert alert = existingAlert.get();
                Incident incident = alert.getIncident();
                return new WebhookResponse("DUPLICATE", alert.getId(), incident != null ? incident.getId() : null);
            }
        }

        // DB Idempotency Check
        Optional<Alert> existingAlert = alertRepository.findByIntegrationIdAndFingerprint(integration.getId(), fingerprint);
        if (existingAlert.isPresent()) {
            Alert alert = existingAlert.get();
            log.info("Duplicate webhook alert received (fingerprint: {}), skipping incident creation", fingerprint);
            redisService.cacheAlert(dedupCacheKey, alert.getId().toString(), 3600);
            Incident incident = alert.getIncident();
            return new WebhookResponse("DUPLICATE", alert.getId(), incident != null ? incident.getId() : null);
        }

        // Create normalized Incident
        Incident incident = new Incident(org, alertData.title(), alertData.severity());
        incident.setDescription(alertData.description());
        incident.setService(integration.getService());
        incident.setSource(integration.getProvider());

        if (integration.getService() != null && integration.getService().getTeam() != null) {
            incident.setTeam(integration.getService().getTeam());
            Optional<User> responder = onCallService.findCurrentResponderUserForTeam(integration.getService().getTeam().getId());
            responder.ifPresent(incident::setAssignee);
        }

        incident = incidentRepository.save(incident);

        // Publish transactional outbox event for IncidentCreated
        outboxService.publishEvent(
                "INCIDENT",
                incident.getId().toString(),
                "IncidentCreated",
                org.getId(),
                java.util.Map.of("incidentId", incident.getId(), "title", incident.getTitle(), "severity", incident.getSeverity().name(), "status", incident.getStatus().name())
        );

        // Record initial timeline event
        IncidentEvent event = new IncidentEvent(
                incident,
                "INCIDENT_CREATED_VIA_WEBHOOK",
                null,
                "{\"provider\":\"" + integration.getProvider() + "\",\"fingerprint\":\"" + fingerprint + "\"" +
                        (incident.getAssignee() != null ? ",\"assignedTo\":\"" + incident.getAssignee().getId() + "\"" : "") + "}"
        );
        incidentEventRepository.save(event);

        // Create & save Alert with DB constraint protection
        Alert alert = new Alert(org, integration, alertData.externalId(), fingerprint, alertData.severity(), rawPayload);
        alert.setIncident(incident);

        try {
            alert = alertRepository.save(alert);
            redisService.cacheAlert(dedupCacheKey, alert.getId().toString(), 3600);

            // Publish transactional outbox event for AlertReceived
            outboxService.publishEvent(
                    "ALERT",
                    alert.getId().toString(),
                    "AlertReceived",
                    org.getId(),
                    java.util.Map.of("alertId", alert.getId(), "integrationId", integration.getId(), "fingerprint", fingerprint, "severity", alertData.severity().name())
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("Data integrity violation on alert saving (duplicate fingerprint), retrieving existing alert");
            Alert duplicate = alertRepository.findByIntegrationIdAndFingerprint(integration.getId(), fingerprint)
                    .orElse(alert);
            redisService.cacheAlert(dedupCacheKey, duplicate.getId().toString(), 3600);
            return new WebhookResponse("DUPLICATE", duplicate.getId(), duplicate.getIncident() != null ? duplicate.getIncident().getId() : incident.getId());
        }

        return new WebhookResponse("INGESTED", alert.getId(), incident.getId());
    }

    private NormalizedAlertData parsePayload(String provider, String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String title = "Webhook Alert";
            String description = rawPayload;
            String externalId = null;
            IncidentSeverity severity = IncidentSeverity.HIGH;

            if (root.has("title")) {
                title = root.get("title").asText();
            } else if (root.has("summary")) {
                title = root.get("summary").asText();
            } else if (root.has("message")) {
                title = root.get("message").asText();
            }

            if (root.has("description")) {
                description = root.get("description").asText();
            } else if (root.has("details")) {
                description = root.get("details").asText();
            }

            if (root.has("id")) {
                externalId = root.get("id").asText();
            } else if (root.has("alertId")) {
                externalId = root.get("alertId").asText();
            }

            if (root.has("severity")) {
                String sevStr = root.get("severity").asText().toUpperCase();
                try {
                    severity = IncidentSeverity.valueOf(sevStr);
                } catch (Exception ignored) {
                    if (sevStr.contains("CRIT")) severity = IncidentSeverity.CRITICAL;
                    else if (sevStr.contains("WARN") || sevStr.contains("MED")) severity = IncidentSeverity.MEDIUM;
                    else if (sevStr.contains("INFO") || sevStr.contains("LOW")) severity = IncidentSeverity.LOW;
                }
            }

            String fingerprintInput = provider + ":" + (externalId != null ? externalId : title + ":" + description);
            String fingerprint = sha256Hex(fingerprintInput);

            return new NormalizedAlertData(title, description, externalId, fingerprint, severity);
        } catch (Exception e) {
            String fingerprint = sha256Hex(provider + ":" + rawPayload);
            return new NormalizedAlertData("Webhook Alert", rawPayload, null, fingerprint, IncidentSeverity.HIGH);
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public record WebhookResponse(String status, java.util.UUID alertId, java.util.UUID incidentId) {}
    private record NormalizedAlertData(String title, String description, String externalId, String fingerprint, IncidentSeverity severity) {}
}
