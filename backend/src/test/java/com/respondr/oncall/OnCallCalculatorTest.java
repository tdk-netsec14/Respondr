package com.respondr.oncall;

import com.respondr.auth.User;
import com.respondr.oncall.dto.RotationRulesDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OnCallCalculatorTest {

    private UUID aliceId;
    private UUID bobId;
    private UUID charlieId;
    private OffsetDateTime rotationStart;

    @BeforeEach
    void setUp() {
        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();
        charlieId = UUID.randomUUID();
        rotationStart = OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void calculatesRotationParticipantDeterministically() {
        // 8-hour cadence in Asia/Kolkata: Alice -> Bob -> Charlie
        RotationRulesDto rules = new RotationRulesDto(
                List.of(aliceId, bobId, charlieId),
                8,
                rotationStart
        );

        // Shift 0 (Hours 0..8): Alice
        OffsetDateTime t0 = rotationStart.plusHours(2);
        Optional<UUID> r0 = OnCallCalculator.calculateResponderAt(rules, "Asia/Kolkata", null, t0);
        assertThat(r0).contains(aliceId);

        // Shift 1 (Hours 8..16): Bob
        OffsetDateTime t1 = rotationStart.plusHours(10);
        Optional<UUID> r1 = OnCallCalculator.calculateResponderAt(rules, "Asia/Kolkata", null, t1);
        assertThat(r1).contains(bobId);

        // Shift 2 (Hours 16..24): Charlie
        OffsetDateTime t2 = rotationStart.plusHours(18);
        Optional<UUID> r2 = OnCallCalculator.calculateResponderAt(rules, "Asia/Kolkata", null, t2);
        assertThat(r2).contains(charlieId);

        // Shift 3 (Hours 24..32): Alice (Wraps around)
        OffsetDateTime t3 = rotationStart.plusHours(26);
        Optional<UUID> r3 = OnCallCalculator.calculateResponderAt(rules, "Asia/Kolkata", null, t3);
        assertThat(r3).contains(aliceId);
    }

    @Test
    void overrideTakesPrecedenceOverScheduledRotation() {
        RotationRulesDto rules = new RotationRulesDto(
                List.of(aliceId, bobId, charlieId),
                8,
                rotationStart
        );

        // At t1 (+10h), scheduled responder is Bob
        OffsetDateTime t1 = rotationStart.plusHours(10);

        // Add override replacing Bob with Charlie for hours 9..12
        User charlieUser = new User("charlie@test.com", "hash", "Charlie");
        try {
            var f = com.respondr.common.persistence.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(charlieUser, charlieId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ScheduleOverride override = new ScheduleOverride(
                null, charlieUser, rotationStart.plusHours(9), rotationStart.plusHours(12), "Covering for Bob");

        Optional<UUID> responderWithOverride = OnCallCalculator.calculateResponderAt(
                rules, "Asia/Kolkata", List.of(override), t1);

        assertThat(responderWithOverride).contains(charlieId);
    }
}
