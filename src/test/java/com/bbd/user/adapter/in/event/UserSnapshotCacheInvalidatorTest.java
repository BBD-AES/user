package com.bbd.user.adapter.in.event;

import com.bbd.user.adapter.out.redis.UserSnapshotCacheEvictor;
import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventMetrics;
import com.bbd.user.config.UserEventProperties;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserSnapshotCacheInvalidatorTest {

    @Test
    void invalidatesSnapshotFromKafkaPayload() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate(false);
        UserSnapshotCacheInvalidator invalidator = invalidator(redisTemplate, meterRegistry);
        String payload = new ObjectMapper().writeValueAsString(event("target-sub"));

        invalidator.invalidate(payload);

        assertEquals("user:snapshot:target-sub", redisTemplate.deletedKey);
        assertEquals(
                1.0,
                meterRegistry.get("bbd.user.snapshot.eviction")
                        .tag("source", "kafka")
                        .tag("result", "success")
                        .counter()
                        .count()
        );
    }

    @Test
    void redisFailureEscapesForKafkaRetryAndDltRouting() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        UserSnapshotCacheInvalidator invalidator =
                invalidator(new RecordingRedisTemplate(true), meterRegistry);
        String payload = new ObjectMapper().writeValueAsString(event("target-sub"));

        assertThrows(IllegalStateException.class, () -> invalidator.invalidate(payload));

        assertEquals(
                1.0,
                meterRegistry.get("bbd.user.snapshot.eviction")
                        .tag("source", "kafka")
                        .tag("result", "failure")
                        .counter()
                        .count()
        );
    }

    private static UserSnapshotCacheInvalidator invalidator(
            StringRedisTemplate redisTemplate,
            SimpleMeterRegistry meterRegistry
    ) {
        UserEventProperties properties = new UserEventProperties();
        properties.setCacheKeyPrefix("user:snapshot:");
        UserSnapshotCacheEvictor cacheEvictor = new UserSnapshotCacheEvictor(
                redisTemplate,
                properties,
                new UserEventMetrics(meterRegistry)
        );
        return new UserSnapshotCacheInvalidator(new ObjectMapper(), cacheEvictor);
    }

    private static UserChangedEvent event(String keycloakSub) {
        return new UserChangedEvent(
                UUID.randomUUID(),
                UserChangeType.USER_AUTHORIZATION_CHANGED,
                Instant.parse("2026-06-21T00:00:00Z"),
                1L,
                keycloakSub,
                "EMP-1",
                UserStatus.ACTIVE,
                UserRole.HQ_STAFF,
                TenancyType.HQ,
                "HQ",
                2L
        );
    }

    private static class RecordingRedisTemplate extends StringRedisTemplate {

        private final boolean fail;
        private String deletedKey;

        private RecordingRedisTemplate(boolean fail) {
            this.fail = fail;
        }

        @Override
        public Boolean delete(String key) {
            if (fail) {
                throw new IllegalStateException("Redis unavailable");
            }

            this.deletedKey = key;
            return true;
        }
    }
}
