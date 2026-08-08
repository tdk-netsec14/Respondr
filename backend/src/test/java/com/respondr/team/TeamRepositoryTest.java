package com.respondr.team;

import com.respondr.AbstractRepositoryTest;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TeamRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private OrganizationRepository orgRepository;

    private Organization org;

    @BeforeEach
    void setUp() {
        org = orgRepository.save(new Organization("Team Test Org", "team-test-org-" + System.nanoTime()));
    }

    @Test
    void savesAndFindsTeam() {
        Team team = teamRepository.save(new Team(org, "Platform", "Platform engineering"));

        assertThat(team.getId()).isNotNull();
        assertThat(team.getCreatedAt()).isNotNull();
        assertThat(teamRepository.findById(team.getId())).isPresent();
    }

    @Test
    void findsByOrganizationId() {
        teamRepository.save(new Team(org, "Backend", null));
        teamRepository.save(new Team(org, "Frontend", null));

        Page<Team> page = teamRepository.findByOrganizationId(org.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).allMatch(t -> t.getOrganization().getId().equals(org.getId()));
    }

    @Test
    void existsByOrgAndName() {
        teamRepository.save(new Team(org, "DevOps", null));
        assertThat(teamRepository.existsByOrganizationIdAndName(org.getId(), "DevOps")).isTrue();
        assertThat(teamRepository.existsByOrganizationIdAndName(org.getId(), "Nobody")).isFalse();
    }
}
