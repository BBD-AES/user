package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.response.UserDirectoryResponse;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.in.GetUserUseCase;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDirectoryControllerTest {

    @Test
    void getsUserDirectoryProfile() {
        GetUserUseCase getUserUseCase = mock(GetUserUseCase.class);
        UserDirectoryController controller = new UserDirectoryController(getUserUseCase);
        UserResult result = user();

        when(getUserUseCase.getById(12L)).thenReturn(result);

        UserDirectoryResponse response = controller.getUser(12L);

        assertEquals(result.userId(), response.userId());
        assertEquals(result.employeeNumber(), response.employeeNumber());
        assertEquals(result.displayName(), response.displayName());
        assertEquals(result.email(), response.email());
        assertEquals(result.position(), response.position());
        assertEquals(result.status(), response.status());
        assertEquals(result.tenancyType(), response.tenancyType());
        assertEquals(result.tenancyName(), response.tenancyName());
        verify(getUserUseCase).getById(12L);
    }

    @Test
    void directoryResponseDoesNotExposeAuthorizationFields() {
        assertFalse(hasRecordComponent("keycloakSub"));
        assertFalse(hasRecordComponent("role"));
        assertFalse(hasRecordComponent("version"));
    }

    private boolean hasRecordComponent(String name) {
        return Arrays.stream(UserDirectoryResponse.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals(name));
    }

    private UserResult user() {
        return new UserResult(
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
}
