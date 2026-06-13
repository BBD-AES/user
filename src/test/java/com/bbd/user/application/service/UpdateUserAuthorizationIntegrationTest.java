package com.bbd.user.application.service;

import com.bbd.user.adapter.out.outbox.UserOutboxJpaRepository;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.UpdateUserAuthorizationUseCase;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 User DB 변경과 Outbox 저장의 실제 영속성 흐름을 검증하는 통합 테스트.

 H2 PostgreSQL 호환 모드에 Flyway V1~V3를 적용하고,
 실제 JPA adapter와 UpdateUserAuthorizationService를 사용한다.

 검증 대상:
 - users row의 @Version이 1에서 2로 증가하는지
 - 같은 처리에서 user_outbox row가 생성되는지
 - Kafka를 켜지 않은 상태이므로 Outbox가 PENDING으로 남는지

 @Transactional로 각 테스트가 끝난 뒤 데이터를 rollback해서 테스트 간 영향을 없앤다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/test-jwks",
        "bbd.user.events.enabled=false"
})
@Transactional
class UpdateUserAuthorizationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UpdateUserAuthorizationUseCase updateUserAuthorizationUseCase;

    @Autowired
    private UserOutboxJpaRepository userOutboxJpaRepository;

    @Test
    void userUpdateAndOutboxAreStoredTogether() {
        insertUser("manager-sub", "EMP-1", UserStatus.ACTIVE, UserRole.HQ_MANAGER, TenancyType.HQ);
        insertUser("target-sub", "EMP-2", UserStatus.PENDING, UserRole.BRANCH_STAFF, TenancyType.BRANCH);

        Long targetId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE keycloak_sub = ?",
                Long.class,
                "target-sub"
        );

        UserSnapshotResult result = updateUserAuthorizationUseCase.updateAuthorization(
                new UpdateUserAuthorizationCommand(
                        "manager-sub",
                        targetId,
                        UserStatus.ACTIVE,
                        UserRole.BRANCH_MANAGER,
                        TenancyType.BRANCH,
                        "강남 지점"
                )
        );

        assertEquals(2L, result.version());
        assertEquals(1L, userOutboxJpaRepository.count());
        assertEquals(
                "PENDING",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM user_outbox WHERE aggregate_id = ?",
                        String.class,
                        targetId
                )
        );
    }

    private void insertUser(
            String keycloakSub,
            String employeeNumber,
            UserStatus status,
            UserRole role,
            TenancyType tenancyType
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    keycloak_sub,
                    employee_number,
                    username,
                    display_name,
                    email,
                    position,
                    status,
                    role,
                    tenancy_type,
                    tenancy_name,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """,
                keycloakSub,
                employeeNumber,
                employeeNumber.toLowerCase(),
                employeeNumber,
                employeeNumber.toLowerCase() + "@example.com",
                "직원",
                status.name(),
                role.name(),
                tenancyType.name(),
                tenancyType == TenancyType.HQ ? "본사" : "지점"
        );
    }
}
