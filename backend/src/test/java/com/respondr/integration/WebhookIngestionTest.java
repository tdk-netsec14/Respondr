package com.respondr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.AbstractIntegrationTest;
import com.respondr.auth.dto.RegisterRequest;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WebhookIngestionTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AlertIntegrationRepository integrationRepository;
    @Autowired private OrganizationRepository organizationRepository;

    private Organization organization;
    private AlertIntegration integration;
    private String secretKey;
    private String integrationKey;

    @BeforeEach
    void setUp() {
        long nano = System.nanoTime();
        organization = organizationRepository.save(new Organization("Webhook Org " + nano, "wh-org-" + nano));
        secretKey = "wh-secret-key-" + nano;
        integrationKey = "prometheus-key-" + nano;

        integration = integrationRepository.save(new AlertIntegration(
                organization, null, "Prometheus Ingestion", integrationKey, "PROMETHEUS", secretKey));
    }

    @Test
    void ingestsWebhookAndDeduplicatesReplayedAlert() throws Exception {
        String payload = "{\"id\":\"alert-101\",\"title\":\"High Database Latency\",\"severity\":\"CRITICAL\",\"description\":\"Postgres latency > 500ms\"}";
        String signature = HmacUtils.calculateHmacSha256(payload, secretKey);

        // First webhook call -> INGESTED
        MvcResult res1 = mockMvc.perform(post("/api/v1/webhooks/" + integrationKey)
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INGESTED"))
                .andExpect(jsonPath("$.alertId").exists())
                .andExpect(jsonPath("$.incidentId").exists())
                .andReturn();

        Map<?, ?> map1 = objectMapper.readValue(res1.getResponse().getContentAsString(), Map.class);
        String incidentId = (String) map1.get("incidentId");

        // Replayed/Duplicate webhook call with identical payload & signature -> DUPLICATE (returns same incidentId)
        MvcResult res2 = mockMvc.perform(post("/api/v1/webhooks/" + integrationKey)
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andReturn();
    }

    @Test
    void rejectsWebhookWithInvalidSignature() throws Exception {
        String payload = "{\"title\":\"Server Down\"}";
        mockMvc.perform(post("/api/v1/webhooks/" + integrationKey)
                        .header("X-Signature", "invalid-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }
}
