CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,

                                     keycloak_sub VARCHAR(100) NOT NULL UNIQUE,
    employee_number VARCHAR(50) NOT NULL UNIQUE,

    username VARCHAR(100),
    display_name VARCHAR(100),
    email VARCHAR(255),
    position VARCHAR(100),

    status VARCHAR(30) NOT NULL,
    role VARCHAR(50) NOT NULL,

    tenancy_type VARCHAR(30) NOT NULL,
    tenancy_id BIGINT,
    tenancy_name VARCHAR(100),

    version BIGINT NOT NULL DEFAULT 1,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_users_keycloak_sub
    ON users (keycloak_sub);

CREATE INDEX IF NOT EXISTS idx_users_employee_number
    ON users (employee_number);