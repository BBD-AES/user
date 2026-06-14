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

/*
 DLT Consumer가 payload를 다시 읽어 Redis Snapshot을 삭제하고,
 실패한 record는 예외를 유지해 listener container가 재시도하도록 하는지 검증한다.
 */
class UserSnapshotDltRecoveryConsumerTest {

    @Test
    void recoversDltRecordAndRecordsMetrics() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        UserEventMetrics metrics = new UserEventMetrics(meterRegistry);
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate(false);
        UserSnapshotDltRecoveryConsumer consumer = consumer(redisTemplate, metrics);
        UserChangedEvent event = event("target-sub");

        consumer.recover(new ObjectMapper().writeValueAsString(event));

        assertEquals("user:snapshot:target-sub", redisTemplate.deletedKey);
        assertEquals(1.0, meterRegistry.get("bbd.user.event.dlt.attempt").counter().count());
        assertEquals(1.0, meterRegistry.get("bbd.user.event.dlt.recovered").counter().count());
    }

    @Test
    void redisFailureEscapesSoDltContainerCanRetry() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        UserEventMetrics metrics = new UserEventMetrics(meterRegistry);
        UserSnapshotDltRecoveryConsumer consumer =
                consumer(new RecordingRedisTemplate(true), metrics);
        String payload = new ObjectMapper().writeValueAsString(event("target-sub"));

        assertThrows(IllegalStateException.class, () -> consumer.recover(payload));

        assertEquals(1.0, meterRegistry.get("bbd.user.event.dlt.attempt").counter().count());
        assertEquals(
                0,
                meterRegistry.find("bbd.user.event.dlt.recovered").counters().size()
        );
    }

    private static UserSnapshotDltRecoveryConsumer consumer(
            StringRedisTemplate redisTemplate,
            UserEventMetrics metrics
    ) {
        UserEventProperties properties = new UserEventProperties();
        UserSnapshotCacheEvictor cacheEvictor =
                new UserSnapshotCacheEvictor(redisTemplate, properties, metrics);
        return new UserSnapshotDltRecoveryConsumer(
                new ObjectMapper(),
                cacheEvictor,
                metrics
        );
    }

    private static UserChangedEvent event(String keycloakSub) {
        return new UserChangedEvent(
                UUID.randomUUID(),
                UserChangeType.USER_AUTHORIZATION_CHANGED,
                Instant.now(),
                1L,
                keycloakSub,
                "EMP-1",
                UserStatus.ACTIVE,
                UserRole.HQ_STAFF,
                TenancyType.HQ,
                "본사",
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
