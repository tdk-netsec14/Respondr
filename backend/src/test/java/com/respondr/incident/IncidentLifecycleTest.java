package com.respondr.incident;

import com.respondr.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentLifecycleTest {

    @Test
    void allowsValidTransitions() {
        // OPEN -> ACKNOWLEDGED / CANCELLED
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED)).doesNotThrowAnyException();
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.OPEN, IncidentStatus.CANCELLED)).doesNotThrowAnyException();

        // ACKNOWLEDGED -> INVESTIGATING / RESOLVED
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING)).doesNotThrowAnyException();
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.ACKNOWLEDGED, IncidentStatus.RESOLVED)).doesNotThrowAnyException();

        // INVESTIGATING -> RESOLVED
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.INVESTIGATING, IncidentStatus.RESOLVED)).doesNotThrowAnyException();

        // RESOLVED -> REOPENED
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.RESOLVED, IncidentStatus.REOPENED)).doesNotThrowAnyException();

        // REOPENED -> ACKNOWLEDGED / INVESTIGATING
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.REOPENED, IncidentStatus.ACKNOWLEDGED)).doesNotThrowAnyException();
        assertThatCode(() -> IncidentService.validateTransition(IncidentStatus.REOPENED, IncidentStatus.INVESTIGATING)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidTransitions() {
        // OPEN -> RESOLVED directly is invalid (must acknowledge/investigate)
        assertThatThrownBy(() -> IncidentService.validateTransition(IncidentStatus.OPEN, IncidentStatus.RESOLVED))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition from OPEN to RESOLVED");

        // CANCELLED -> ACKNOWLEDGED is invalid
        assertThatThrownBy(() -> IncidentService.validateTransition(IncidentStatus.CANCELLED, IncidentStatus.ACKNOWLEDGED))
                .isInstanceOf(ApiException.class);

        // CLOSED -> REOPENED is invalid
        assertThatThrownBy(() -> IncidentService.validateTransition(IncidentStatus.CLOSED, IncidentStatus.REOPENED))
                .isInstanceOf(ApiException.class);
    }
}
