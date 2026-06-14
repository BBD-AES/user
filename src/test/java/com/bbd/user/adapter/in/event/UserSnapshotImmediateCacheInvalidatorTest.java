package com.bbd.user.adapter.in.event;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventProperties;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 AFTER_COMMIT Redis 무효화 로직의 순수 동작을 검증한다.

 실제 transaction phase 연결은 Spring이 담당한다.
 여기서는 cache key 생성과 Redis 장애가 사용자 변경 결과로 전파되지 않는지를 확인한다.
 */
class UserSnapshotImmediateCacheInvalidatorTest {

    @Test
    void evictsSnapshotUsingKeycloakSub() {
        UserEventProperties properties = new UserEventProperties();
        properties.setCacheKeyPrefix("user:snapshot:");
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate(false);
        UserSnapshotImmediateCacheInvalidator invalidator =
                new UserSnapshotImmediateCacheInvalidator(redisTemplate, properties);

        invalidator.invalidate(event("target-sub"));

        assertEquals("user:snapshot:target-sub", redisTemplate.deletedKey);
    }

    @Test
    void redisFailureDoesNotEscapeAfterCommitListener() {
        UserEventProperties properties = new UserEventProperties();
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate(true);
        UserSnapshotImmediateCacheInvalidator invalidator =
                new UserSnapshotImmediateCacheInvalidator(redisTemplate, properties);

        assertDoesNotThrow(() -> invalidator.invalidate(event("target-sub")));
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
