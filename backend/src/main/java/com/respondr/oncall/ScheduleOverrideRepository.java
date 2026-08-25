package com.respondr.oncall;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverride, UUID> {

    List<ScheduleOverride> findByScheduleIdOrderByStartAtAsc(UUID scheduleId);

    @Query("SELECT o FROM ScheduleOverride o WHERE o.schedule.id = :scheduleId AND o.startAt <= :time AND o.endAt > :time ORDER BY o.createdAt DESC")
    List<ScheduleOverride> findActiveOverridesAtTime(@Param("scheduleId") UUID scheduleId, @Param("time") OffsetDateTime time);

    @Query("SELECT o FROM ScheduleOverride o WHERE o.schedule.id = :scheduleId AND o.startAt < :endAt AND o.endAt > :startAt")
    List<ScheduleOverride> findOverlappingOverrides(
            @Param("scheduleId") UUID scheduleId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );
}
