-- midPoint reconciliation에서 userName을 안정적인 검색 조건으로 사용하기 위한 unique index.
-- PostgreSQL과 H2 모두 NULL 값은 여러 건 허용하므로 기존 username 미지정 사용자는 영향을 받지 않는다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username
    ON users (username);