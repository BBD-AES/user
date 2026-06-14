package com.bbd.user.adapter.out.redis;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventMetrics;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/*
 AFTER_COMMIT, Kafka Consumer, DLT Consumer가 공통으로 사용하는 Redis Snapshot 삭제 adapter.

 삭제 key 규칙과 성공/실패 지표를 한 곳에서 관리해 세 경로가 서로 다르게 동작하지 않게 한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class UserSnapshotCacheEvictor {

    private final StringRedisTemplate redisTemplate;
    private final UserEventProperties properties;
    private final UserEventMetrics metrics;

    /*
     keycloakSub로 security-core와 동일한 Snapshot key를 만든 뒤 삭제한다.
     호출 경로는 source tag로 남기고, 실패는 호출자에게 다시 던져 각 경로의 복구 정책을 실행한다.
     */
    public String evict(UserChangedEvent event, String source) {
        String cacheKey = properties.getCacheKeyPrefix() + event.keycloakSub();

        try {
            redisTemplate.delete(cacheKey);
            metrics.recordSnapshotEviction(source, "success");
            return cacheKey;
        } catch (RuntimeException exception) {
            metrics.recordSnapshotEviction(source, "failure");
            throw exception;
        }
    }
}
