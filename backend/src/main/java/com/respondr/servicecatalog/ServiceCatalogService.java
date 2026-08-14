package com.respondr.servicecatalog;

import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.TenantContextHolder;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import com.respondr.servicecatalog.dto.CreateServiceRequest;
import com.respondr.servicecatalog.dto.ServiceResponse;
import com.respondr.servicecatalog.dto.UpdateServiceRequest;
import com.respondr.team.Team;
import com.respondr.team.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for the service catalog.
 * Enforces strict tenant isolation and role-based access control.
 */
@Service
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository,
                                 OrganizationRepository organizationRepository,
                                 TeamRepository teamRepository) {
        this.serviceRepository = serviceRepository;
        this.organizationRepository = organizationRepository;
        this.teamRepository = teamRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<ServiceResponse> listByOrg(UUID orgId, Pageable pageable) {
        assertOrgAccess(orgId);
        return serviceRepository.findByOrganizationId(orgId, pageable)
                .map(ServiceResponse::from);
    }

    public ServiceResponse getById(UUID id) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.service(id.toString()));
        assertOrgAccess(service.getOrganization().getId());
        return ServiceResponse.from(service);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    public ServiceResponse create(UUID orgId, CreateServiceRequest req) {
        assertOrgAccess(orgId);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(orgId.toString()));

        if (serviceRepository.existsByOrganizationIdAndKey(orgId, req.key())) {
            throw ConflictException.serviceKeyTaken(req.key());
        }

        Team team = null;
        if (req.teamId() != null) {
            team = teamRepository.findById(req.teamId())
                    .orElseThrow(() -> ResourceNotFoundException.team(req.teamId().toString()));
            assertOrgAccess(team.getOrganization().getId());
        }

        ServiceEntity service = new ServiceEntity(org, team, req.name(), req.key(), req.description());
        return ServiceResponse.from(serviceRepository.save(service));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    public ServiceResponse update(UUID id, UpdateServiceRequest req) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.service(id.toString()));
        assertOrgAccess(service.getOrganization().getId());

        Team team = null;
        if (req.teamId() != null) {
            team = teamRepository.findById(req.teamId())
                    .orElseThrow(() -> ResourceNotFoundException.team(req.teamId().toString()));
            assertOrgAccess(team.getOrganization().getId());
        }

        service.setName(req.name());
        service.setDescription(req.description());
        service.setActive(req.active());
        service.setTeam(team);
        return ServiceResponse.from(serviceRepository.save(service));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public void delete(UUID id) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.service(id.toString()));
        assertOrgAccess(service.getOrganization().getId());
        serviceRepository.delete(service);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertOrgAccess(UUID orgId) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        if (!currentOrgId.equals(orgId)) {
            throw new AccessDeniedException("Access denied to organization resources: " + orgId);
        }
    }
}
