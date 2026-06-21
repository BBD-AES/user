package com.bbd.user.adapter.out.persistence;

import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPersistenceAdapterTest {

    @Test
    void mapsKeycloakSubUniqueViolationToDuplicateUserError() {
        UserJpaRepository userJpaRepository = mock(UserJpaRepository.class);
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(userJpaRepository);

        when(userJpaRepository.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(uniqueViolation("duplicate key value violates unique constraint users_keycloak_sub_key"));

        ApiException exception = assertThrows(ApiException.class, () -> adapter.save(newUser()));

        assertEquals(ErrorCode.USER_DUPLICATED_KEYCLOAK_SUB, exception.getErrorCode());
    }

    @Test
    void mapsEmployeeNumberUniqueViolationToDuplicateUserError() {
        UserJpaRepository userJpaRepository = mock(UserJpaRepository.class);
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(userJpaRepository);

        when(userJpaRepository.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(uniqueViolation("Unique index or primary key violation on USERS(EMPLOYEE_NUMBER)"));

        ApiException exception = assertThrows(ApiException.class, () -> adapter.save(newUser()));

        assertEquals(ErrorCode.USER_DUPLICATED_EMPLOYEE_NUMBER, exception.getErrorCode());
    }

    @Test
    void keepsUnidentifiedDataIntegrityViolationUnchanged() {
        UserJpaRepository userJpaRepository = mock(UserJpaRepository.class);
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(userJpaRepository);
        DataIntegrityViolationException violation =
                new DataIntegrityViolationException("not null constraint failed: users.role");

        when(userJpaRepository.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(violation);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> adapter.save(newUser())
        );

        assertSame(violation, thrown);
    }

    @Test
    void keepsUnidentifiedUniqueViolationUnchanged() {
        UserJpaRepository userJpaRepository = mock(UserJpaRepository.class);
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(userJpaRepository);
        DataIntegrityViolationException violation = uniqueViolation(
                "Unique index or primary key violation on USERS(ID); "
                        + "SQL [insert into users (employee_number, keycloak_sub) values (?, ?)]"
        );

        when(userJpaRepository.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(violation);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> adapter.save(newUser())
        );

        assertSame(violation, thrown);
    }

    private DataIntegrityViolationException uniqueViolation(String message) {
        return new DataIntegrityViolationException(message, new SQLException(message, "23505"));
    }

    private User newUser() {
        return new User(
                null,
                "keycloak-sub",
                "EMP-001",
                "User",
                "user@example.com",
                "Staff",
                UserStatus.PENDING,
                UserRole.BRANCH_STAFF,
                TenancyType.BRANCH,
                "Gangnam Branch",
                1L
        );
    }
}
