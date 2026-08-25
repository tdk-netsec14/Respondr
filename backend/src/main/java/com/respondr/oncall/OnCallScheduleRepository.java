package com.respondr.oncall;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OnCallScheduleRepository extends JpaRepository<OnCallSchedule, UUID> {

    List<OnCallSchedule> findByTeamIdAndActiveTrue(UUID teamId);

    Page<OnCallSchedule> findByOrganizationId(UUID orgId, Pageable pageable);

    Optional<OnCallSchedule> findByIdAndOrganizationId(UUID id, UUID orgId);
}
