package com.respondr.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Page<Team> findByOrganizationId(UUID orgId, Pageable pageable);

    List<Team> findByOrganizationId(UUID orgId);

    boolean existsByOrganizationIdAndName(UUID orgId, String name);
}
