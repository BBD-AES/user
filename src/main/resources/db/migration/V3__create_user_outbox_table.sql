-- User DB 변경과 Kafka 발행 사이의 유실 구간을 막기 위한 Transactional Outbox 테이블.
-- users 테이블 변경과 이 테이블 INSERT는 같은 DB 트랜잭션에서 수행한다.
-- Kafka 발행은 commit 이후 scheduled publisher가 PENDING row를 조회해서 처리한다.
CREATE TABLE IF NOT EXISTS user_outbox (
    -- UserChangedEvent의 전역 고유 ID. 중복 이벤트 추적 기준으로 사용할 수 있다.
    event_id UUID PRIMARY KEY,

    -- 현재는 USER만 사용하지만 이후 다른 aggregate와 구분할 수 있도록 저장한다.
    aggregate_type VARCHAR(50) NOT NULL,

    -- 변경된 users.id.
    aggregate_id BIGINT NOT NULL,

    -- USER_CREATED, USER_AUTHORIZATION_CHANGED, USER_DEACTIVATED.
    event_type VARCHAR(50) NOT NULL,

    -- Kafka message key로 사용할 keycloakSub.
    event_key VARCHAR(100) NOT NULL,

    -- UserChangedEvent를 JSON으로 직렬화한 실제 Kafka payload.
    payload TEXT NOT NULL,

    -- PENDING은 미발행/재시도 대상, PUBLISHED는 broker 응답 확인 완료 상태.
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Kafka 발행 실패 횟수.
    attempts INTEGER NOT NULL DEFAULT 0,

    -- 업무 변경 이벤트가 발생한 시각.
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Kafka 발행 성공을 확인한 시각.
    published_at TIMESTAMP WITH TIME ZONE,

    -- 마지막 발행 오류. 운영 장애 확인용이며 최대 1000자까지만 저장한다.
    last_error VARCHAR(1000)
    );

-- Publisher가 PENDING 이벤트를 발생 순서대로 빠르게 조회하기 위한 index.
CREATE INDEX IF NOT EXISTS idx_user_outbox_pending
    ON user_outbox (status, occurred_at);
