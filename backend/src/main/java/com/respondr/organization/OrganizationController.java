package com.respondr.organization;

import com.respondr.organization.dto.CreateOrganizationRequest;
import com.respondr.organization.dto.OrganizationResponse;
import com.respondr.organization.dto.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for organization CRUD.
 * Authentication and tenant isolation will be enforced by Spring Security in Phase 2.
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public Page<OrganizationResponse> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return organizationService.listOrganizations(pageable);
    }

    @GetMapping("/{id}")
    public OrganizationResponse getById(@PathVariable UUID id) {
        return organizationService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    public OrganizationResponse getBySlug(@PathVariable String slug) {
        return organizationService.getBySlug(slug);
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest req) {
        OrganizationResponse body = organizationService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PatchMapping("/{id}")
    public OrganizationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest req) {
        return organizationService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        organizationService.delete(id);
    }
}
