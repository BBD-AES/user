package com.bbd.user.adapter.in.event;

import com.bbd.user.adapter.out.redis.UserSnapshotCacheEvictor;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/*
 메인 Consumer의 재시도를 모두 소진한 USER_CHANGED DLT record를 복구한다.

 Redis가 계속 장애 상태라면 DLT offset을 진행하지 않고 낮은 빈도로 계속 재시도한다.
 Redis가 복구되면 Snapshot key를 삭제하고 해당 DLT record를 정상 commit한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class UserSnapshotDltRecoveryConsumer {

    private final ObjectMapper objectMapper;
    private final UserSnapshotCacheEvictor cacheEvictor;
    private final UserEventMetrics metrics;

    /*
     성공해야만 DLT offset이 commit된다.
     Redis 삭제 또는 JSON 역직렬화가 실패하면 예외를 전파해 DLT 전용 ErrorHandler가 다시 처리한다.
     */
    @KafkaListener(
            topics = "${bbd.user.events.topic:erp.user.changed.v1}${bbd.user.events.dlt-suffix:.DLT}",
            groupId = "${bbd.user.events.dlt-recovery-consumer-group:user-snapshot-dlt-recovery-v1}",
            containerFactory = "userEventDltKafkaListenerContainerFactory"
    )
    public void recover(String payload) throws Exception {
        metrics.recordDltAttempt();

        UserChangedEvent event = objectMapper.readValue(payload, UserChangedEvent.class);
        String cacheKey = cacheEvictor.evict(event, "dlt");

        metrics.recordDltRecovered();
        log.info(
                "DLT 사용자 스냅샷 복구를 완료했습니다. eventId={}, keycloakSub={}, cacheKey={}",
                event.eventId(),
                event.keycloakSub(),
                cacheKey
        );
    }
}
