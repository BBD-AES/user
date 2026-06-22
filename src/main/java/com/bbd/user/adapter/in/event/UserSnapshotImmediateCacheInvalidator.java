package com.bbd.user.adapter.in.event;

import com.bbd.user.adapter.out.snapshot.SnapshotInvalidationOutboxProcessor;
import com.bbd.user.application.event.UserChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 사용자 상태, 역할, 소속 변경 transaction이 commit된 직후 Redis Snapshot을 삭제한다.

 Kafka Outbox Publisher의 polling 주기를 기다리지 않고 변경사항을 즉시 반영하기 위한 경로다.
 여기서 Redis 삭제가 실패해도 snapshot_invalidation_outbox row는 PENDING으로 남아 있다.
 이후 Scheduler가 같은 Snapshot을 다시 삭제해 최종 일관성을 맞춘다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bbd.user.events", name = "enabled", havingValue = "true")
public class UserSnapshotImmediateCacheInvalidator {

    private final SnapshotInvalidationOutboxProcessor processor;

    /*
     AFTER_COMMIT이므로 User 변경과 두 Outbox 저장이 rollback된 경우에는 실행되지 않는다.
     Redis 삭제 결과는 processor가 snapshot_invalidation_outbox row에 기록한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(UserChangedEvent event) {
        processor.invalidateAfterCommit(event);
    }
}
