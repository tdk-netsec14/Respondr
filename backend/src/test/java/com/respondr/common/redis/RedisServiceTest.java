package com.respondr.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void tryAcquireRateLimit_returnsTrueWhenWithinLimit() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = redisService.tryAcquireRateLimit("key1", 5, 60);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(eq("rate_limit:key1"), any(Duration.class));
    }

    @Test
    void tryAcquireRateLimit_returnsFalseWhenLimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(6L);

        boolean allowed = redisService.tryAcquireRateLimit("key1", 5, 60);

        assertThat(allowed).isFalse();
    }

    @Test
    void tryAcquireRateLimit_fallsBackToTrue_whenRedisThrowsException() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        boolean allowed = redisService.tryAcquireRateLimit("key1", 5, 60);

        assertThat(allowed).isTrue();
    }

    @Test
    void isAlertCached_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey("alert_dedup:fp123")).thenReturn(true);

        boolean cached = redisService.isAlertCached("fp123");

        assertThat(cached).isTrue();
    }

    @Test
    void isAlertCached_fallsBackToFalse_whenRedisThrowsException() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        boolean cached = redisService.isAlertCached("fp123");

        assertThat(cached).isFalse();
    }

    @Test
    void acquireLock_returnsTrueWhenLockAcquired() {
        when(valueOperations.setIfAbsent(eq("lock:test"), eq("val1"), any(Duration.class))).thenReturn(true);

        boolean lockAcquired = redisService.acquireLock("test", "val1", 30);

        assertThat(lockAcquired).isTrue();
    }

    @Test
    void acquireLock_fallsBackToTrue_whenRedisThrowsException() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenThrow(new RuntimeException("Redis failure"));

        boolean lockAcquired = redisService.acquireLock("test", "val1", 30);

        assertThat(lockAcquired).isTrue();
    }
}
