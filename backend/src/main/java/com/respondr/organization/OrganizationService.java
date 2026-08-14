package com.respondr.organization;

import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.TenantContextHolder;
import com.respondr.organization.dto.CreateOrganizationRequest;
import com.respondr.organization.dto.OrganizationResponse;
import com.respondr.organization.dto.UpdateOrganizationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for organization lifecycle management.
 * Enforces tenant context isolation on all queries and mutations.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<OrganizationResponse> listOrganizations(Pageable pageable) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        Organization org = organizationRepository.findById(currentOrgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(currentOrgId.toString()));
        return new PageImpl<>(List.of(OrganizationResponse.from(org)), pageable, 1);
    }

    public OrganizationResponse getById(UUID id) {
        assertTenantAccess(id);
        return organizationRepository.findById(id)
                .map(OrganizationResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.organization(id.toString()));
    }

    public OrganizationResponse getBySlug(String slug) {
        Organization org = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.organization(slug));
        assertTenantAccess(org.getId());
        return OrganizationResponse.from(org);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest req) {
        if (organizationRepository.existsBySlug(req.slug())) {
            throw ConflictException.slugTaken(req.slug());
        }
        Organization org = new Organization(req.name(), req.slug());
        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest req) {
        assertTenantAccess(id);
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.organization(id.toString()));
        org.setName(req.name());
        return OrganizationResponse.from(organizationRepository.save(org));
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public void delete(UUID id) {
        assertTenantAccess(id);
        if (!organizationRepository.existsById(id)) {
            throw ResourceNotFoundException.organization(id.toString());
        }
        organizationRepository.deleteById(id);
    }

    // ── Tenant Isolation Assertion ─────────────────────────────────────────────

    private void assertTenantAccess(UUID targetOrgId) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        if (!currentOrgId.equals(targetOrgId)) {
            throw new AccessDeniedException("Access denied to organization " + targetOrgId);
        }
    }
}
