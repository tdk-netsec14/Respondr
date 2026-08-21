package com.respondr.integration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertIntegrationRepository extends JpaRepository<AlertIntegration, UUID> {

    Optional<AlertIntegration> findByKey(String key);

    Page<AlertIntegration> findByOrganizationId(UUID orgId, Pageable pageable);

    boolean existsByOrganizationIdAndKey(UUID orgId, String key);
}
