# ADR-0001: Modular Monolith over Microservices

**Date:** 2026-09-26  
**Status:** Accepted  
**Deciders:** Engineering Lead

## Context

Respondr is a greenfield project. We need an architecture that allows rapid development, clear bounded contexts, and future scalability without premature complexity.

## Decision

Start with a **modular monolith** using Java package-per-domain under `com.respondr`. Each domain (alert, incident, oncall, etc.) is fully self-contained with its own models, repositories, services, and controllers. Cross-domain communication uses internal Spring application events (not REST or Kafka) where possible.

## Consequences

**Positive:**
- Single deployable unit — simpler CI/CD, no distributed tracing complexity at project start.
- Strong type safety across domain boundaries — no serialization overhead.
- Refactor to independent services is straightforward because boundaries are enforced by package structure.

**Negative:**
- Cannot scale domains independently. Mitigated by selective extraction when a domain becomes a bottleneck.
- All teams deploy together. Mitigated by feature flags and trunk-based development.

## Trigger for Extraction

A domain will be extracted into a separate service ONLY when two or more of these conditions are met:
1. It requires independent horizontal scaling beyond the monolith's JVM heap.
2. It has a fundamentally different SLA (e.g., webhook ingestion vs. analytics).
3. A separate team owns it exclusively.
