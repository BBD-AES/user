package com.bbd.user.adapter.out.redis;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventMetrics;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/*
 Redis에 저장된 User Snapshot 캐시를 삭제하는 공통 adapter.

 이 adapter는 다음 세 경로에서 공통으로 사용된다.

 1. AFTER_COMMIT 즉시 삭제 경로
    - User 변경 트랜잭션이 commit된 직후 Redis Snapshot을 빠르게 삭제한다.
    - Kafka Outbox Publisher의 polling 주기를 기다리지 않아 stale cache 노출 시간을 줄인다.

 2. Kafka Consumer 경로
    - Outbox에 저장된 UserChangedEvent가 Kafka로 발행된 뒤,
      Consumer가 동일한 Snapshot을 다시 삭제한다.
    - AFTER_COMMIT 경로가 실패했거나 실행되지 못한 경우의 복구 경로다.

 3. DLT Consumer 경로
    - Kafka 처리 실패가 누적되어 DLT로 이동한 이벤트를 재처리할 때 사용한다.
    - 운영자가 장애 원인을 해결한 뒤 실패 이벤트를 다시 처리할 수 있게 한다.

 Redis DEL 연산은 멱등적이다.
 즉, 이미 삭제된 key를 다시 삭제해도 문제가 없으므로
 AFTER_COMMIT, Kafka Consumer, DLT Consumer가 같은 key를 중복 삭제해도 안전하다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class UserSnapshotCacheEvictor {

    private final StringRedisTemplate redisTemplate;
    // 여기서는 Redis Key 앞부분을 설정에서 가져오는 역할
    private final UserEventProperties properties;
    // 삭제의 성공/실패를 기록(source와 함께)
    private final UserEventMetrics metrics;

    /*
     UserChangedEvent의 keycloakSub를 기준으로 security-core와 동일한 Redis key를 만든다.

     예)
     cacheKeyPrefix = "user:snapshot:"
     keycloakSub    = "abc-123"
     cacheKey       = "user:snapshot:abc-123"

     source는 이 삭제 요청이 어디에서 발생했는지 구분하기 위한 tag다.

     예)
     - "after_commit" : 트랜잭션 commit 직후 즉시 삭제
     - "kafka"        : Kafka Consumer 재처리 삭제
     - "dlt"          : DLT Consumer 복구 삭제

     삭제 성공/실패는 metric으로 기록한다.
     이를 통해 운영 중 어떤 경로에서 Redis 삭제 실패가 발생했는지 확인할 수 있다.

     이 메서드는 Redis 삭제 실패를 내부에서 삼키지 않고 다시 던진다.
     이유는 호출 경로마다 실패 처리 정책이 다르기 때문이다.

     예)
     - AFTER_COMMIT 경로는 이미 DB commit이 끝났으므로 로그만 남기고 Kafka 복구 경로에 맡긴다.
     - Kafka Consumer 경로는 예외를 던져 재시도 또는 DLT 이동 정책을 타게 한다.
     - DLT Consumer 경로는 운영 복구 실패로 보고 별도 로그/지표를 남길 수 있다.
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