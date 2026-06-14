package com.bbd.user.adapter.in.event;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/*
 USER_CHANGED 이벤트를 소비해서 Redis UserSnapshot을 무효화하는 inbound event adapter.

 사용자 상태, 역할, 소속이 변경되면 기존 Redis Snapshot은 오래된 인가 정보를 가진다.
 이 Consumer는 해당 key를 삭제하고,
 다음 MSA 요청에서 bbd-security-core가 User Service를 다시 조회해 최신 Snapshot을 저장하게 한다.

 현재는 cache를 직접 update하지 않고 evict 방식을 사용한다.
 UserSnapshot 생성 책임을 User Service 조회 API 하나로 유지해
 event payload와 cache 구조가 달라지는 문제를 줄이기 위해서다.

 Redis DELETE는 같은 key에 여러 번 실행해도 결과가 같으므로
 Kafka의 at-least-once 중복 전달을 안전하게 처리할 수 있다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "enabled",
        havingValue = "true"
)
public class UserSnapshotCacheInvalidator {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final UserEventProperties properties;

    @KafkaListener(
            topics = "${bbd.user.events.topic:erp.user.changed.v1}",
            groupId = "${bbd.user.events.snapshot-consumer-group:user-snapshot-projector-v1}"
    )
    public void invalidate(String payload) throws Exception {
        // Outbox에 저장한 JSON payload를 공통 event contract로 역직렬화한다.
        UserChangedEvent event = objectMapper.readValue(payload, UserChangedEvent.class);

        // 예: user:snapshot:{keycloakSub}
        stringRedisTemplate.delete(properties.getCacheKeyPrefix() + event.keycloakSub());
    }
}
