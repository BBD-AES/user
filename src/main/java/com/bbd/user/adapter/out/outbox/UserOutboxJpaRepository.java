package com.bbd.user.adapter.out.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/*
 user_outbox 조회/저장용 Spring Data JPA Repository.

 Publisher가 여러 User Service 인스턴스에서 동시에 실행될 수 있으므로
 단순 findAll이 아니라 PostgreSQL의 FOR UPDATE SKIP LOCKED를 사용한다.

 한 인스턴스가 잡은 row는 다른 인스턴스가 기다리지 않고 건너뛰기 때문에
 같은 Outbox row를 동시에 발행하려는 경쟁을 줄일 수 있다.
 */
public interface UserOutboxJpaRepository extends JpaRepository<UserOutboxJpaEntity, UUID> {

    // Prometheus에서 아직 발행되지 않은 Outbox 적체량을 확인할 때 사용한다.
    long countByStatus(UserOutboxStatus status);

    /*
     오래된 PENDING 이벤트부터 batchSize만큼 가져온다.
     이 query는 @Transactional 범위 안에서 실행되어야 row lock이 유지된다.
     */
    @Query(value = """
            SELECT *
            FROM user_outbox
            WHERE status = 'PENDING'
            ORDER BY occurred_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<UserOutboxJpaEntity> findPendingForPublish(@Param("batchSize") int batchSize);
}
