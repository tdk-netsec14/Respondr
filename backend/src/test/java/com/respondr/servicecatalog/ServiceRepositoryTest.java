package com.respondr.servicecatalog;

import com.respondr.AbstractRepositoryTest;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private OrganizationRepository orgRepository;

    private Organization org;

    @BeforeEach
    void setUp() {
        org = orgRepository.save(new Organization("Svc Test Org", "svc-test-org-" + System.nanoTime()));
    }

    @Test
    void savesAndFindsService() {
        ServiceEntity svc = serviceRepository.save(
                new ServiceEntity(org, null, "Payments API", "payments-api", "Handles payments"));

        assertThat(svc.getId()).isNotNull();
        assertThat(svc.isActive()).isTrue();
        assertThat(serviceRepository.findById(svc.getId())).isPresent();
    }

    @Test
    void findsByOrgAndKey() {
        serviceRepository.save(new ServiceEntity(org, null, "Auth Service", "auth-svc", null));

        Optional<ServiceEntity> found = serviceRepository.findByOrganizationIdAndKey(org.getId(), "auth-svc");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Auth Service");
    }

    @Test
    void existsByOrgAndKeyReturnsFalseForMissing() {
        assertThat(serviceRepository.existsByOrganizationIdAndKey(org.getId(), "unknown-key")).isFalse();
    }
}
