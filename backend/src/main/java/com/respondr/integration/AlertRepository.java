package com.respondr.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByIntegrationIdAndFingerprint(UUID integrationId, String fingerprint);

    boolean existsByIntegrationIdAndFingerprint(UUID integrationId, String fingerprint);
}
