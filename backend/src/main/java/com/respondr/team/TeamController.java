package com.respondr.team;

import com.respondr.team.dto.CreateTeamRequest;
import com.respondr.team.dto.TeamResponse;
import com.respondr.team.dto.UpdateTeamRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for team CRUD scoped to an organization.
 */
@RestController
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/api/v1/organizations/{orgId}/teams")
    public Page<TeamResponse> listByOrg(
            @PathVariable UUID orgId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return teamService.listByOrg(orgId, pageable);
    }

    @GetMapping("/api/v1/teams/{id}")
    public TeamResponse getById(@PathVariable UUID id) {
        return teamService.getById(id);
    }

    @PostMapping("/api/v1/organizations/{orgId}/teams")
    public ResponseEntity<TeamResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateTeamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(orgId, req));
    }

    @PatchMapping("/api/v1/teams/{id}")
    public TeamResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeamRequest req) {
        return teamService.update(id, req);
    }

    @DeleteMapping("/api/v1/teams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamService.delete(id);
    }
}
