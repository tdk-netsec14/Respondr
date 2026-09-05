package com.respondr.escalation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EscalationStepRepository extends JpaRepository<EscalationStep, UUID> {
    List<EscalationStep> findByPolicyIdOrderByStepOrderAsc(UUID policyId);
    Optional<EscalationStep> findByPolicyIdAndStepOrder(UUID policyId, int stepOrder);
    void deleteByPolicyId(UUID policyId);
}
