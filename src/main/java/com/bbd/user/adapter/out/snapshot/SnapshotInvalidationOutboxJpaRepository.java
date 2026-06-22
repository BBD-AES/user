package com.bbd.user.adapter.out.snapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/*
 snapshot_invalidation_outbox 조회/저장용 Spring Data JPA Repository.

 여러 User Service 인스턴스의 retry scheduler가 동시에 실행될 수 있으므로
 PostgreSQL의 FOR UPDATE SKIP LOCKED로 같은 row의 중복 처리를 줄인다.
 */
public interface SnapshotInvalidationOutboxJpaRepository
        extends JpaRepository<SnapshotInvalidationOutboxJpaEntity, UUID> {

    long countByStatus(SnapshotInvalidationOutboxStatus status);

    @Query(value = """
            SELECT *
            FROM snapshot_invalidation_outbox
            WHERE status = 'PENDING'
            ORDER BY occurred_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SnapshotInvalidationOutboxJpaEntity> findPendingForRetry(
            @Param("batchSize") int batchSize
    );
}
