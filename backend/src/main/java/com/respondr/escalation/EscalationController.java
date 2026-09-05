package com.respondr.escalation;

import com.respondr.escalation.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/escalation-policies")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping
    @PreAuthorize("@tenantSecurity.hasOrgRole(#orgId, 'ADMIN') or @tenantSecurity.hasOrgRole(#orgId, 'INCIDENT_MANAGER')")
    public ResponseEntity<PolicyResponse> createPolicy(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreatePolicyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalationService.createPolicy(orgId, req));
    }

    @GetMapping("/{policyId}")
    @PreAuthorize("@tenantSecurity.hasOrgPermission(#orgId, 'VIEW')")
    public PolicyResponse getPolicy(
            @PathVariable UUID orgId,
            @PathVariable UUID policyId) {
        return escalationService.getPolicy(orgId, policyId);
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.hasOrgPermission(#orgId, 'VIEW')")
    public Page<PolicyResponse> listPolicies(
            @PathVariable UUID orgId,
            @PageableDefault(size = 20) Pageable pageable) {
        return escalationService.listPolicies(orgId, pageable);
    }

    @PatchMapping("/{policyId}")
    @PreAuthorize("@tenantSecurity.hasOrgRole(#orgId, 'ADMIN') or @tenantSecurity.hasOrgRole(#orgId, 'INCIDENT_MANAGER')")
    public PolicyResponse updatePolicy(
            @PathVariable UUID orgId,
            @PathVariable UUID policyId,
            @Valid @RequestBody UpdatePolicyRequest req) {
        return escalationService.updatePolicy(orgId, policyId, req);
    }

    @DeleteMapping("/{policyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@tenantSecurity.hasOrgRole(#orgId, 'ADMIN')")
    public void deletePolicy(
            @PathVariable UUID orgId,
            @PathVariable UUID policyId) {
        escalationService.deletePolicy(orgId, policyId);
    }

    // ── Steps Endpoints ───────────────────────────────────────────────────────

    @PostMapping("/{policyId}/steps")
    @PreAuthorize("@tenantSecurity.hasOrgRole(#orgId, 'ADMIN') or @tenantSecurity.hasOrgRole(#orgId, 'INCIDENT_MANAGER')")
    public ResponseEntity<StepResponse> addStep(
            @PathVariable UUID orgId,
            @PathVariable UUID policyId,
            @Valid @RequestBody CreateStepRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalationService.addStep(orgId, policyId, req));
    }

    @GetMapping("/{policyId}/steps")
    @PreAuthorize("@tenantSecurity.hasOrgPermission(#orgId, 'VIEW')")
    public List<StepResponse> getSteps(
            @PathVariable UUID orgId,
            @PathVariable UUID policyId) {
        return escalationService.getSteps(orgId, policyId);
    }

    @DeleteMapping("/{policyId}/steps/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@tenantSecurity.hasOrgRole(#orgId, 'ADMIN') or @tenantSecurity.hasOrgRole(#orgId, 'INCIDENT_MANAGER')")
    public void deleteStep(
            @PathVariable UUID orgId,
            @PathVariable UUID policyId,
            @PathVariable UUID stepId) {
        escalationService.deleteStep(orgId, policyId, stepId);
    }
}
