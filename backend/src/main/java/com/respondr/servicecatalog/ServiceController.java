package com.respondr.servicecatalog;

import com.respondr.servicecatalog.dto.CreateServiceRequest;
import com.respondr.servicecatalog.dto.ServiceResponse;
import com.respondr.servicecatalog.dto.UpdateServiceRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for service catalog CRUD.
 */
@RestController
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/api/v1/organizations/{orgId}/services")
    public Page<ServiceResponse> listByOrg(
            @PathVariable UUID orgId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return serviceCatalogService.listByOrg(orgId, pageable);
    }

    @GetMapping("/api/v1/services/{id}")
    public ServiceResponse getById(@PathVariable UUID id) {
        return serviceCatalogService.getById(id);
    }

    @PostMapping("/api/v1/organizations/{orgId}/services")
    public ResponseEntity<ServiceResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateServiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceCatalogService.create(orgId, req));
    }

    @PatchMapping("/api/v1/services/{id}")
    public ServiceResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest req) {
        return serviceCatalogService.update(id, req);
    }

    @DeleteMapping("/api/v1/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        serviceCatalogService.delete(id);
    }
}
