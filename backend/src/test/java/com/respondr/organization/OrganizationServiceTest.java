package com.respondr.organization;

import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.TenantContextHolder;
import com.respondr.organization.dto.CreateOrganizationRequest;
import com.respondr.organization.dto.OrganizationResponse;
import com.respondr.organization.dto.UpdateOrganizationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository repository;

    @InjectMocks
    private OrganizationService service;

    private Organization organization;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        organization = new Organization("Acme", "acme");
        orgId = UUID.randomUUID();
        try {
            var field = com.respondr.common.persistence.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(organization, orgId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TenantContextHolder.setContext(new TenantContextHolder.TenantContext(
                orgId, UUID.randomUUID(), MemberRole.OWNER, "owner@acme.com"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void create_savesAndReturnsOrganization() {
        when(repository.existsBySlug("acme")).thenReturn(false);
        when(repository.save(any(Organization.class))).thenReturn(organization);

        OrganizationResponse response = service.create(new CreateOrganizationRequest("Acme", "acme"));

        assertThat(response.name()).isEqualTo("Acme");
        assertThat(response.slug()).isEqualTo("acme");
        verify(repository).save(any(Organization.class));
    }

    @Test
    void create_throwsConflict_whenSlugTaken() {
        when(repository.existsBySlug("acme")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateOrganizationRequest("Acme", "acme")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("acme");

        verify(repository, never()).save(any());
    }

    @Test
    void getById_returnsOrganization_whenExists() {
        when(repository.findById(orgId)).thenReturn(Optional.of(organization));

        OrganizationResponse response = service.getById(orgId);

        assertThat(response.name()).isEqualTo("Acme");
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID unknownOrg = orgId;
        when(repository.findById(unknownOrg)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(unknownOrg))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_updatesNameOnly() {
        when(repository.findById(orgId)).thenReturn(Optional.of(organization));
        when(repository.save(any(Organization.class))).thenReturn(organization);

        OrganizationResponse response = service.update(orgId, new UpdateOrganizationRequest("Acme Updated"));

        assertThat(response).isNotNull();
        verify(repository).save(organization);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(repository.existsById(orgId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(orgId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesById_whenExists() {
        when(repository.existsById(orgId)).thenReturn(true);

        service.delete(orgId);

        verify(repository).deleteById(orgId);
    }
}
