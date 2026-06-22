package com.bbd.user.application.service;

import com.bbd.user.application.model.UserSearchCondition;
import com.bbd.user.application.model.UserSearchResult;
import com.bbd.user.application.port.in.SearchUsersUseCase;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 RDS users 테이블 기준 목록 조회 조건을 검증한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/test-jwks",
        "bbd.user.events.enabled=false",
        "bbd.security.enabled=false"
})
@Transactional
class SearchUsersIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SearchUsersUseCase searchUsersUseCase;

    @BeforeEach
    void setUp() {
        insertUser(
                "sub-hq-manager",
                "EMP-001",
                "본사 관리자",
                UserRole.HQ_MANAGER,
                TenancyType.HQ
        );
        insertUser(
                "sub-hq-staff",
                "EMP-002",
                "본사 직원",
                UserRole.HQ_STAFF,
                TenancyType.HQ
        );
        insertUser(
                "sub-branch-staff",
                "BR-001",
                "강남 지점 직원",
                UserRole.BRANCH_STAFF,
                TenancyType.BRANCH
        );
    }

    @Test
    void searchesAllUsers() {
        UserSearchResult result = searchUsersUseCase.search(
                new UserSearchCondition(null, null, null, null, 0, 20)
        );

        assertEquals(3L, result.totalElements());
        assertEquals(3, result.users().size());
    }

    @Test
    void searchesByEmployeeNumberAndName() {
        UserSearchResult byEmployeeNumber = searchUsersUseCase.search(
                new UserSearchCondition("EMP", null, null, null, 0, 20)
        );
        UserSearchResult byName = searchUsersUseCase.search(
                new UserSearchCondition(null, "강남", null, null, 0, 20)
        );

        assertEquals(2L, byEmployeeNumber.totalElements());
        assertEquals(1L, byName.totalElements());
        assertEquals("BR-001", byName.users().getFirst().employeeNumber());
    }

    @Test
    void searchesByRoleAndTenancyTogether() {
        UserSearchResult result = searchUsersUseCase.search(
                new UserSearchCondition(
                        null,
                        null,
                        UserRole.HQ_STAFF,
                        TenancyType.HQ,
                        0,
                        20
                )
        );

        assertEquals(1L, result.totalElements());
        assertEquals("EMP-002", result.users().getFirst().employeeNumber());
    }

    private void insertUser(
            String keycloakSub,
            String employeeNumber,
            String displayName,
            UserRole role,
            TenancyType tenancyType
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    keycloak_sub,
                    employee_number,
                    display_name,
                    email,
                    position,
                    status,
                    role,
                    tenancy_type,
                    tenancy_name,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """,
                keycloakSub,
                employeeNumber,
                displayName,
                employeeNumber + "@example.com",
                "직원",
                UserStatus.ACTIVE.name(),
                role.name(),
                tenancyType.name(),
                tenancyType == TenancyType.HQ ? "본사" : "강남 지점"
        );
    }
}
