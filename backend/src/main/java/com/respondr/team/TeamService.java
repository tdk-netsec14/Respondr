package com.respondr.team;

import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.TenantContextHolder;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import com.respondr.team.dto.CreateTeamRequest;
import com.respondr.team.dto.TeamResponse;
import com.respondr.team.dto.UpdateTeamRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for team lifecycle management.
 * Enforces strict tenant isolation and role-based access control.
 */
@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;

    public TeamService(TeamRepository teamRepository, OrganizationRepository organizationRepository) {
        this.teamRepository = teamRepository;
        this.organizationRepository = organizationRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<TeamResponse> listByOrg(UUID orgId, Pageable pageable) {
        assertOrgAccess(orgId);
        return teamRepository.findByOrganizationId(orgId, pageable)
                .map(TeamResponse::from);
    }

    public TeamResponse getById(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.team(id.toString()));
        assertOrgAccess(team.getOrganization().getId());
        return TeamResponse.from(team);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    public TeamResponse create(UUID orgId, CreateTeamRequest req) {
        assertOrgAccess(orgId);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(orgId.toString()));
        Team team = new Team(org, req.name(), req.description());
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    public TeamResponse update(UUID id, UpdateTeamRequest req) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.team(id.toString()));
        assertOrgAccess(team.getOrganization().getId());
        team.setName(req.name());
        team.setDescription(req.description());
        return TeamResponse.from(teamRepository.save(team));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public void delete(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.team(id.toString()));
        assertOrgAccess(team.getOrganization().getId());
        teamRepository.delete(team);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertOrgAccess(UUID orgId) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        if (!currentOrgId.equals(orgId)) {
            throw new AccessDeniedException("Access denied to organization resources: " + orgId);
        }
    }
}
