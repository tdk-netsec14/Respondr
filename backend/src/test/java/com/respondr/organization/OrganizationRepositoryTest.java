package com.respondr.organization;

import com.respondr.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private OrganizationRepository repository;

    @Test
    void savesAndFindsById() {
        Organization org = new Organization("Acme Corp", "acme-corp");
        Organization saved = repository.save(org);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<Organization> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().getSlug()).isEqualTo("acme-corp");
    }

    @Test
    void findsBySlug() {
        repository.save(new Organization("Beta Inc", "beta-inc"));

        Optional<Organization> found = repository.findBySlug("beta-inc");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Beta Inc");
    }

    @Test
    void returnsEmptyForUnknownSlug() {
        Optional<Organization> found = repository.findBySlug("does-not-exist");
        assertThat(found).isEmpty();
    }

    @Test
    void existsBySlugReturnsTrueWhenPresent() {
        repository.save(new Organization("Gamma LLC", "gamma-llc"));
        assertThat(repository.existsBySlug("gamma-llc")).isTrue();
    }

    @Test
    void existsBySlugReturnsFalseWhenAbsent() {
        assertThat(repository.existsBySlug("nobody-here")).isFalse();
    }

    @Test
    void deletesById() {
        Organization org = repository.save(new Organization("Delete Me", "delete-me"));
        repository.deleteById(org.getId());
        assertThat(repository.findById(org.getId())).isEmpty();
    }
}
