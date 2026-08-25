package com.respondr.oncall.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RotationRulesDto(
        List<UUID> participantUserIds,
        long shiftLengthHours,
        OffsetDateTime rotationStartAt
) {}
