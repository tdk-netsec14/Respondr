package com.respondr.escalation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentEscalationStateRepository extends JpaRepository<IncidentEscalationState, UUID> {
    Optional<IncidentEscalationState> findByIncidentId(UUID incidentId);
    List<IncidentEscalationState> findByStatusAndNextExecutionAtBefore(EscalationStatus status, OffsetDateTime cutoff);
}
