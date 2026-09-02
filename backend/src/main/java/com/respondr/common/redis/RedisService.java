package com.respondr.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service providing Redis rate limiting, fast alert deduplication caching,
 * and short-lived distributed locking for escalation steps.
 *
 * <p><strong>Graceful Fallback:</strong> All Redis calls catch exceptions.
 * If Redis is unavailable, operations fall back to PostgreSQL authority safely.
 */
@Service
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * Fixed-window rate-limiter for webhook endpoints.
     * Returns true if request is within limits or if Redis is unavailable.
     */
    public boolean tryAcquireRateLimit(String key, int maxRequests, int windowSeconds) {
        if (redisTemplate == null) return true;
        try {
            String redisKey = "rate_limit:" + key;
            Long current = redisTemplate.opsForValue().increment(redisKey);
            if (current != null && current == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }
            return current == null || current <= maxRequests;
        } catch (Exception e) {
            log.warn("Redis rate-limiting unavailable, falling back to allow: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Fast-path alert deduplication check.
     * Returns true if alert fingerprint is cached in Redis.
     */
    public boolean isAlertCached(String fingerprintKey) {
        if (redisTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("alert_dedup:" + fingerprintKey));
        } catch (Exception e) {
            log.warn("Redis alert dedup cache unavailable, falling back to DB check: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Caches alert fingerprint in Redis with a TTL.
     */
    public void cacheAlert(String fingerprintKey, String alertId, int ttlSeconds) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set("alert_dedup:" + fingerprintKey, alertId, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to cache alert in Redis: {}", e.getMessage());
        }
    }

    /**
     * Short-lived distributed lock for escalation step processing.
     * Returns true if lock is acquired or if Redis is unavailable (DB idempotency boundary applies).
     */
    public boolean acquireLock(String lockKey, String lockValue, long leaseTimeSeconds) {
        if (redisTemplate == null) return true;
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    "lock:" + lockKey, lockValue, Duration.ofSeconds(leaseTimeSeconds));
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis distributed lock unavailable, falling back to DB idempotency: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Releases distributed lock safely.
     */
    public void releaseLock(String lockKey, String lockValue) {
        if (redisTemplate == null) return;
        try {
            String key = "lock:" + lockKey;
            String currentVal = redisTemplate.opsForValue().get(key);
            if (lockValue.equals(currentVal)) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("Failed to release Redis lock: {}", e.getMessage());
        }
    }
}
