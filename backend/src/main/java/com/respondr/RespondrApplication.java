package com.respondr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Respondr — real-time, multi-tenant incident management and on-call platform.
 *
 * <p>Architecture: modular monolith (package-per-domain). Packages:
 * <ul>
 *   <li>auth              — authentication &amp; JWT issuance</li>
 *   <li>organization      — tenant / organisation management</li>
 *   <li>team              — team membership &amp; roles</li>
 *   <li>servicecatalog    — service registry</li>
 *   <li>integration       — inbound webhook integrations</li>
 *   <li>alert             — alert ingestion, deduplication, verification</li>
 *   <li>incident          — incident lifecycle management</li>
 *   <li>oncall            — on-call schedule &amp; responder resolution</li>
 *   <li>escalation        — escalation policy engine</li>
 *   <li>notification      — multi-channel notification dispatch</li>
 *   <li>realtime          — WebSocket / STOMP real-time collaboration</li>
 *   <li>event             — internal domain event bus</li>
 *   <li>search            — Elasticsearch-backed full-text search</li>
 *   <li>analytics         — MTTA / MTTR metrics &amp; reporting</li>
 *   <li>audit             — immutable audit log</li>
 *   <li>common.security   — Spring Security + JWT + RBAC shared config</li>
 *   <li>common.exception  — global exception handling</li>
 *   <li>common.web        — shared REST utilities</li>
 *   <li>common.persistence — shared JPA base classes &amp; repositories</li>
 *   <li>common.observability — OpenTelemetry, Prometheus, logging config</li>
 * </ul>
 */
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RespondrApplication {

    public static void main(String[] args) {
        SpringApplication.run(RespondrApplication.class, args);
    }
}
