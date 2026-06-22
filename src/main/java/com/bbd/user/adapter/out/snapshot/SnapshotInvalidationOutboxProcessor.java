package com.bbd.user.adapter.out.snapshot;

import com.bbd.user.adapter.out.redis.UserSnapshotCacheEvictor;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/*
 Redis Snapshot 무효화 outbox 처리기.

 AFTER_COMMIT 즉시 삭제와 scheduled retry가 모두 같은 table row를 갱신한다.
 재시도 상한을 넘은 row는 FAILED로 격리되며, 이 상태가 DB 기반 DLT 역할을 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class SnapshotInvalidationOutboxProcessor {

    private final SnapshotInvalidationOutboxJpaRepository repository;
    private final UserSnapshotCacheEvictor cacheEvictor;
    private final UserEventProperties properties;

    /*
     AFTER_COMMIT listener에서 호출된다.
     원래 사용자 변경 transaction은 이미 끝났으므로 REQUIRES_NEW로 outbox 상태만 갱신한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateAfterCommit(UserChangedEvent event) {
        repository.findById(event.eventId())
                .ifPresentOrElse(
                        outbox -> evict(outbox, "after_commit"),
                        () -> log.warn(
                                "사용자 스냅샷 무효화 outbox row를 찾지 못했습니다. eventId={}, keycloakSub={}",
                                event.eventId(),
                                event.keycloakSub()
                        )
                );
    }

    @Scheduled(fixedDelayString = "${bbd.user.events.snapshot-invalidation-retry-delay-ms:5000}")
    @Transactional
    public void retryPendingInvalidations() {
        List<SnapshotInvalidationOutboxJpaEntity> pending =
                repository.findPendingForRetry(properties.getSnapshotInvalidationBatchSize());

        for (SnapshotInvalidationOutboxJpaEntity outbox : pending) {
            evict(outbox, "snapshot_outbox");
        }
    }

    private void evict(SnapshotInvalidationOutboxJpaEntity outbox, String source) {
        try {
            String cacheKey = cacheEvictor.evict(outbox.getKeycloakSub(), source);
            outbox.markDone(Instant.now());
            log.info(
                    "사용자 스냅샷을 삭제했습니다. source={}, eventId={}, keycloakSub={}, cacheKey={}",
                    source,
                    outbox.getEventId(),
                    outbox.getKeycloakSub(),
                    cacheKey
            );
        } catch (RuntimeException exception) {
            outbox.markFailedAttempt(
                    exception,
                    properties.getSnapshotInvalidationMaxAttempts()
            );
            log.warn(
                    "사용자 스냅샷 삭제에 실패했습니다. source={}, eventId={}, keycloakSub={}, attempts={}, status={}",
                    source,
                    outbox.getEventId(),
                    outbox.getKeycloakSub(),
                    outbox.getAttempts(),
                    outbox.getStatus(),
                    exception
            );
        }
    }
}
