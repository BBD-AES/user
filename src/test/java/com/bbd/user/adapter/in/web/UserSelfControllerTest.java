package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.response.UserSnapshotResponse;
import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.GetUserSnapshotUseCase;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSelfControllerTest {

    @Test
    void getsCurrentUserSnapshotByJwtSubject() {
        GetUserSnapshotUseCase getUserSnapshotUseCase = mock(GetUserSnapshotUseCase.class);
        UserSelfController controller = new UserSelfController(getUserSnapshotUseCase);
        UserSnapshotResult result = snapshot();

        when(getUserSnapshotUseCase.getSnapshotByKeycloakSub("keycloak-sub"))
                .thenReturn(result);

        UserSnapshotResponse response = controller.getMe(jwt("keycloak-sub"));

        assertEquals(result.userId(), response.userId());
        assertEquals(result.keycloakSub(), response.keycloakSub());
        assertEquals(result.employeeNumber(), response.employeeNumber());
        assertEquals(result.displayName(), response.displayName());
        assertEquals(result.email(), response.email());
        assertEquals(result.position(), response.position());
        assertEquals(result.status(), response.status());
        assertEquals(result.role(), response.role());
        assertEquals(result.tenancyType(), response.tenancyType());
        assertEquals(result.tenancyName(), response.tenancyName());
        assertEquals(result.version(), response.version());
        verify(getUserSnapshotUseCase).getSnapshotByKeycloakSub("keycloak-sub");
    }

    @Test
    void rejectsMissingPrincipal() {
        GetUserSnapshotUseCase getUserSnapshotUseCase = mock(GetUserSnapshotUseCase.class);
        UserSelfController controller = new UserSelfController(getUserSnapshotUseCase);

        ApiException exception = assertThrows(ApiException.class, () -> controller.getMe(null));

        assertEquals(ErrorCode.AUTH_UNAUTHENTICATED, exception.getErrorCode());
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", subject)
                .build();
    }

    private UserSnapshotResult snapshot() {
        return new UserSnapshotResult(
                12L,
                "keycloak-sub",
                "BR002",
                "Jung Minsu",
                "minsu@example.com",
                "Mechanic",
                UserStatus.ACTIVE,
                UserRole.BRANCH_STAFF,
                TenancyType.BRANCH,
                "Gangnam Branch 1",
                7L
        );
    }
}
