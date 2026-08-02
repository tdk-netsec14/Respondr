# ADR-0002: Java 21 with Spring Boot 3.3

**Date:** 2026-09-26  
**Status:** Accepted  
**Deciders:** Engineering Lead

## Context

We need a stable, long-term-supported JVM stack with modern features (virtual threads, records, sealed classes) that integrates well with the Spring ecosystem.

## Decision

Use **Java 21 (LTS)** with **Spring Boot 3.3.x**. Enable virtual threads (`spring.threads.virtual.enabled=true`) in Phase 1 for improved I/O throughput without the complexity of reactive programming.

## Consequences

**Positive:**
- Java 21 LTS support until 2031. Spring Boot 3.3 supported until 2025+.
- Virtual threads (Project Loom) provide Netty-level concurrency with blocking-style code.
- Records and sealed classes simplify DTOs and domain event modelling.

**Negative:**
- Some libraries may not yet be Loom-aware. Mitigation: verify with Testcontainers load tests in Phase 11.
