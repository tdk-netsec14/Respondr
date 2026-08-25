package com.respondr.oncall;

import com.respondr.oncall.dto.RotationRulesDto;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OnCallCalculator {

    private OnCallCalculator() {}

    /**
     * Determines the on-call user ID at timestamp {@code targetTime}.
     * Checks active overrides first. If no override applies, calculates shift from rotation rules.
     */
    public static Optional<UUID> calculateResponderAt(
            RotationRulesDto rules,
            String timezone,
            List<ScheduleOverride> activeOverrides,
            OffsetDateTime targetTime) {

        // 1. Check for active override at targetTime
        if (activeOverrides != null) {
            for (ScheduleOverride override : activeOverrides) {
                if (!targetTime.isBefore(override.getStartAt()) && targetTime.isBefore(override.getEndAt())) {
                    return Optional.of(override.getUser().getId());
                }
            }
        }

        // 2. Rotation calculation
        if (rules == null || rules.participantUserIds() == null || rules.participantUserIds().isEmpty()) {
            return Optional.empty();
        }

        List<UUID> participants = rules.participantUserIds();
        long shiftHours = rules.shiftLengthHours() > 0 ? rules.shiftLengthHours() : 24;
        OffsetDateTime rotationStart = rules.rotationStartAt() != null ? rules.rotationStartAt() : targetTime;

        ZoneId zone = ZoneId.of(timezone != null ? timezone : "UTC");
        ZonedDateTime zTarget = targetTime.atZoneSameInstant(zone);
        ZonedDateTime zStart = rotationStart.atZoneSameInstant(zone);

        long secondsElapsed = Duration.between(zStart, zTarget).getSeconds();
        long shiftSeconds = shiftHours * 3600;

        if (secondsElapsed < 0) {
            // Target time before rotation start
            long shiftIndexNegative = (Math.abs(secondsElapsed) + shiftSeconds - 1) / shiftSeconds;
            long modIndex = (participants.size() - (shiftIndexNegative % participants.size())) % participants.size();
            return Optional.of(participants.get((int) modIndex));
        }

        long shiftIndex = (secondsElapsed / shiftSeconds) % participants.size();
        return Optional.of(participants.get((int) shiftIndex));
    }
}
