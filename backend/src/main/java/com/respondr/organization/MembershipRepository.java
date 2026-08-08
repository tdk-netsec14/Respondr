package com.respondr.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByOrganizationId(UUID orgId);

    Optional<Membership> findByOrganizationIdAndUserId(UUID orgId, UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID orgId, UUID userId);
}
