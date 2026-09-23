package com.respondr.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByOrgId(UUID orgId, Pageable pageable);

    List<Notification> findByStatusAndAttemptsLessThan(NotificationStatus status, int maxAttempts);
}
