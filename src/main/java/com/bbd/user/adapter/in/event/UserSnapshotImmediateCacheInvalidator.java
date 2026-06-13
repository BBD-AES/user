package com.bbd.user.adapter.in.event;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 사용자 상태, 역할, 소속 변경 transaction이 commit된 직후 Redis Snapshot을 삭제한다.

 Kafka Outbox Publisher의 polling 주기를 기다리지 않고 변경사항을 즉시 반영하기 위한 경로다.
 여기서 Redis 삭제가 실패해도 Outbox event는 DB에 남아 있다.
 이후 Kafka Consumer가 같은 Snapshot을 다시 삭제해 최종 일관성을 맞춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class UserSnapshotImmediateCacheInvalidator {

    private final StringRedisTemplate redisTemplate;
    private final UserEventProperties properties;

    /*
     AFTER_COMMIT이므로 User 변경과 Outbox 저장이 rollback된 경우에는 실행되지 않는다.

     Redis 장애는 이미 commit된 사용자 변경의 실패 사유가 아니다.
     예외를 외부로 전파하지 않고 로그를 남긴 뒤 Kafka 복구 경로에 맡긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(UserChangedEvent event) {
        String cacheKey = properties.getCacheKeyPrefix() + event.keycloakSub();

        try {
            redisTemplate.delete(cacheKey);
            log.info(
                    "사용자 스냅샷을 커밋 직후 삭제했습니다. keycloakSub={}, cacheKey={}",
                    event.keycloakSub(),
                    cacheKey
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "사용자 스냅샷 즉시 삭제에 실패했습니다. Kafka 이벤트를 통해 다시 삭제합니다. "
                            + "keycloakSub={}, cacheKey={}",
                    event.keycloakSub(),
                    cacheKey,
                    exception
            );
        }
    }
}
