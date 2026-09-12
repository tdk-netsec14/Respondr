package com.respondr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.common.exception.ApiException;
import com.respondr.common.exception.ErrorCode;
import com.respondr.common.redis.RedisService;
import com.respondr.event.OutboxService;
import com.respondr.incident.IncidentEventRepository;
import com.respondr.incident.IncidentRepository;
import com.respondr.oncall.OnCallService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookRateLimitTest {

    @Mock private AlertIntegrationRepository integrationRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentEventRepository incidentEventRepository;
    @Mock private OnCallService onCallService;
    @Mock private ObjectMapper objectMapper;
    @Mock private RedisService redisService;
    @Mock private OutboxService outboxService;

    @InjectMocks private WebhookIngestionService webhookIngestionService;

    @Test
    void processWebhook_throwsTooManyRequests_whenRateLimitExceeded() {
        when(redisService.tryAcquireRateLimit(eq("integration-key-1"), anyInt(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> webhookIngestionService.processWebhook("integration-key-1", null, "{}"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    org.assertj.core.api.Assertions.assertThat(apiEx.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    org.assertj.core.api.Assertions.assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
                });
    }
}
