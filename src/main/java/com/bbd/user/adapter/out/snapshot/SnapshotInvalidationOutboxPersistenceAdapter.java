package com.bbd.user.adapter.out.snapshot;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.port.out.RecordSnapshotInvalidationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 RecordSnapshotInvalidationPort의 DB outbox 구현체.

 User 변경과 같은 @Transactional 범위에 참여해서 Redis Snapshot 삭제 작업을
 PENDING으로 저장한다. Redis 장애나 프로세스 종료가 있어도 scheduler가 이 row를
 기준으로 다시 삭제를 시도할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class SnapshotInvalidationOutboxPersistenceAdapter implements RecordSnapshotInvalidationPort {

    private final SnapshotInvalidationOutboxJpaRepository repository;

    @Override
    public void record(UserChangedEvent event) {
        repository.save(SnapshotInvalidationOutboxJpaEntity.pending(event));
    }
}
