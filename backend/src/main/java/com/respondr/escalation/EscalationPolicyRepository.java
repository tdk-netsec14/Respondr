package com.respondr.escalation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscalationPolicyRepository extends JpaRepository<EscalationPolicy, UUID> {
    Page<EscalationPolicy> findByOrganizationId(UUID orgId, Pageable pageable);
    List<EscalationPolicy> findByOrganizationIdAndTeamId(UUID orgId, UUID teamId);
    Optional<EscalationPolicy> findByIdAndOrganizationId(UUID id, UUID orgId);
}
