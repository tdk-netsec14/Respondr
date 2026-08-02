# Architecture Overview

## System Design

Respondr is built as a **modular monolith** using a package-per-domain structure under `com.respondr`. This allows clear bounded-context separation while avoiding the operational complexity of microservices during early development. Service extraction is performed selectively only if justified by independent scaling or team autonomy needs.

## Key Quality Attributes

| Attribute | Target |
|---|---|
| Availability | 99.9% uptime |
| MTTA | < 2 minutes alert-to-notification |
| MTTR | Tracked per incident, reported via analytics |
| Latency | p99 API < 200 ms |
| Throughput | 10,000 alerts/minute ingestion |
| Tenancy | Full data isolation per organisation |

## Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                      Respondr Backend                    │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────────┐ │
│  │   auth   │  │   alert    │  │      incident        │ │
│  └──────────┘  └────────────┘  └──────────────────────┘ │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────────┐ │
│  │  oncall  │  │ escalation │  │    notification      │ │
│  └──────────┘  └────────────┘  └──────────────────────┘ │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────────┐ │
│  │  event   │  │  realtime  │  │      analytics       │ │
│  └──────────┘  └────────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────────────────┘
         │              │              │
    PostgreSQL        Kafka          Redis
         │              │              │
   Elasticsearch   Prometheus      Grafana
```

## Data Flow

1. External monitoring tool → POST `/api/v1/integrations/{id}/webhook`
2. `IntegrationService` validates signature, maps payload → `AlertEvent`
3. `AlertService` deduplicates, creates or updates `Alert`
4. Kafka event `alert.created` triggers `IncidentService`
5. `IncidentService` resolves on-call responder via `OncallService`
6. `NotificationService` dispatches PagerDuty / Slack / email
7. `EscalationService` polls for unacknowledged incidents → re-notifies
8. `RealtimeService` broadcasts status changes via WebSocket to dashboards
9. `AnalyticsService` computes MTTA/MTTR from incident timestamps

## ADR Index

See [../adr/](../adr/) for individual Architecture Decision Records.
