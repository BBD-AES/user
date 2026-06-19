package com.bbd.user.application.service;

import com.bbd.user.adapter.out.outbox.UserOutboxJpaRepository;
import com.bbd.user.application.model.CreateProvisionedUserCommand;
import com.bbd.user.application.model.ProvisionedUserResult;
import com.bbd.user.application.port.in.ManageProvisionedUserUseCase;
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
 SCIM 사용자 생성과 Transactional Outbox 저장이 실제 Flyway/JPA schema에서 함께 동작하는지 검증한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scim-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/test-jwks",
        "bbd.user.events.enabled=false",
        "bbd.user.scim.enabled=false"
})
@Transactional
class ManageProvisionedUserIntegrationTest {

    @Autowired
    private ManageProvisionedUserUseCase manageProvisionedUserUseCase;

    @Autowired
    private UserOutboxJpaRepository userOutboxJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scimCreateStoresPendingUserAndOutboxTogether() {
        ProvisionedUserResult result = manageProvisionedUserUseCase.create(
                new CreateProvisionedUserCommand(
                        "scim-sub-1",
                        "SCIM-001",
                        "SCIM 사용자",
                        "scim001@example.com",
                        "직원",
                        UserRole.BRANCH_STAFF,
                        TenancyType.BRANCH,
                        "강남 지점",
                        true
                )
        );

        assertEquals(UserStatus.PENDING, result.status());
        assertEquals(
                "PENDING",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM users WHERE id = ?",
                        String.class,
                        result.userId()
                )
        );
        assertEquals(1L, userOutboxJpaRepository.count());
        assertEquals(
                "USER_CREATED",
                jdbcTemplate.queryForObject(
                        "SELECT event_type FROM user_outbox WHERE aggregate_id = ?",
                        String.class,
                        result.userId()
                )
        );
    }
}
