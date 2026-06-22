-- Redis UserSnapshot 삭제는 User Service 내부 책임으로 처리한다.
-- 사용자 변경 트랜잭션 안에서 삭제해야 할 snapshot key 기준값을 먼저 기록하고,
-- commit 이후 즉시 삭제 및 scheduled retry가 이 테이블을 기준으로 동작한다.
CREATE TABLE IF NOT EXISTS snapshot_invalidation_outbox (
    -- UserChangedEvent와 같은 event_id를 사용해 user_outbox와 추적 단위를 맞춘다.
    event_id UUID PRIMARY KEY,

    -- 변경된 users.id.
    aggregate_id BIGINT NOT NULL,

    -- USER_CREATED, USER_PROFILE_CHANGED, USER_AUTHORIZATION_CHANGED, USER_DEACTIVATED.
    event_type VARCHAR(50) NOT NULL,

    -- Redis UserSnapshot key를 만들 때 사용할 keycloakSub.
    keycloak_sub VARCHAR(100) NOT NULL,

    -- PENDING은 삭제/재시도 대상, DONE은 Redis 삭제 완료, FAILED는 재시도 상한 초과.
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Redis 삭제 실패 횟수.
    attempts INTEGER NOT NULL DEFAULT 0,

    -- 사용자 변경 이벤트가 발생한 시각.
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Redis 삭제 성공을 확인한 시각.
    invalidated_at TIMESTAMP WITH TIME ZONE,

    -- 마지막 Redis 삭제 오류. 운영 장애 확인용이며 최대 1000자까지만 저장한다.
    last_error VARCHAR(1000)
    );

-- Scheduler가 PENDING 삭제 작업을 발생 순서대로 빠르게 조회하기 위한 index.
CREATE INDEX IF NOT EXISTS idx_snapshot_invalidation_outbox_pending
    ON snapshot_invalidation_outbox (status, occurred_at);
