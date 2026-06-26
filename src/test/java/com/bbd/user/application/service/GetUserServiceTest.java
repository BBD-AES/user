package com.bbd.user.application.service;

import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUserServiceTest {

    @Test
    void getsUserById() {
        StubLoadUserPort loadUserPort = new StubLoadUserPort(user());
        GetUserService service = new GetUserService(loadUserPort);

        UserResult result = service.getById(12L);

        assertEquals(12L, result.userId());
        assertEquals("EMP-12", result.employeeNumber());
        assertEquals("사용자 12", result.displayName());
        assertEquals("user12@example.com", result.email());
        assertEquals("직원", result.position());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(UserRole.BRANCH_STAFF, result.role());
        assertEquals(TenancyType.BRANCH, result.tenancyType());
        assertEquals("강남 지점", result.tenancyName());
        assertEquals(3L, result.version());
    }

    @Test
    void throwsUserNotFoundWhenMissing() {
        StubLoadUserPort loadUserPort = new StubLoadUserPort(null);
        GetUserService service = new GetUserService(loadUserPort);

        ApiException exception = assertThrows(ApiException.class, () -> service.getById(404L));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    private static User user() {
        return new User(
                12L,
                "keycloak-sub",
                "EMP-12",
                "사용자 12",
                "user12@example.com",
                "직원",
                UserStatus.ACTIVE,
                UserRole.BRANCH_STAFF,
                TenancyType.BRANCH,
                "강남 지점",
                3L
        );
    }

    private static class StubLoadUserPort implements LoadUserPort {

        private final User user;

        private StubLoadUserPort(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findByKeycloakSub(String keycloakSub) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(user)
                    .filter(found -> found.id().equals(userId));
        }
    }
}
