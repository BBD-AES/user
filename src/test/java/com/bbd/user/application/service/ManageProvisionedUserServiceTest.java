package com.bbd.user.application.service;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.model.CreateProvisionedUserCommand;
import com.bbd.user.application.model.UpdateProvisionedUserCommand;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.RecordUserChangedEventPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 midPoint provisioning의 application 규칙을 검증한다.
 */
class ManageProvisionedUserServiceTest {

    @Test
    void activeSourceUserIsCreatedAsPendingAndRecordsCreatedEvent() {
        InMemoryUserPorts userPorts = new InMemoryUserPorts();
        RecordingEventPort eventPort = new RecordingEventPort();
        RecordingPublisher publisher = new RecordingPublisher();
        ManageProvisionedUserService service =
                new ManageProvisionedUserService(userPorts, userPorts, eventPort, publisher);

        UserResult result = service.create(createCommand("sub-1", "EMP-1", true));

        assertEquals(UserStatus.PENDING, result.status());
        assertEquals(1L, result.userId());
        assertEquals(UserChangeType.USER_CREATED, eventPort.recorded.eventType());
        assertEquals(eventPort.recorded, publisher.published);
    }

    @Test
    void inactiveSourceUserIsCreatedAsInactive() {
        InMemoryUserPorts userPorts = new InMemoryUserPorts();
        ManageProvisionedUserService service =
                new ManageProvisionedUserService(userPorts, userPorts, event -> {
                }, event -> {
                });

        UserResult result = service.create(createCommand("sub-1", "EMP-1", false));

        assertEquals(UserStatus.INACTIVE, result.status());
    }

    @Test
    void sourceDeactivationCreatesDeactivatedEvent() {
        InMemoryUserPorts userPorts = new InMemoryUserPorts();
        User active = user(1L, "sub-1", "EMP-1", UserStatus.ACTIVE, 3L);
        userPorts.put(active);
        RecordingEventPort eventPort = new RecordingEventPort();
        ManageProvisionedUserService service =
                new ManageProvisionedUserService(userPorts, userPorts, eventPort, event -> {
                });

        UserResult result = service.update(
                new UpdateProvisionedUserCommand(
                        1L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false
                )
        );

        assertEquals(UserStatus.INACTIVE, result.status());
        assertEquals(UserChangeType.USER_DEACTIVATED, eventPort.recorded.eventType());
        assertEquals(4L, result.version());
    }

    @Test
    void returningInactiveUserRequiresApprovalAgain() {
        InMemoryUserPorts userPorts = new InMemoryUserPorts();
        userPorts.put(user(1L, "sub-1", "EMP-1", UserStatus.INACTIVE, 3L));
        ManageProvisionedUserService service =
                new ManageProvisionedUserService(userPorts, userPorts, event -> {
                }, event -> {
                });

        UserResult result = service.update(
                new UpdateProvisionedUserCommand(
                        1L,
                        null,
                        null,
                        null,
                        null,
                        UserRole.HQ_STAFF,
                        TenancyType.HQ,
                        "본사",
                        true
                )
        );

        assertEquals(UserStatus.PENDING, result.status());
        assertEquals(UserRole.HQ_STAFF, result.role());
        assertEquals(TenancyType.HQ, result.tenancyType());
    }

    @Test
    void duplicateKeycloakSubIsRejected() {
        InMemoryUserPorts userPorts = new InMemoryUserPorts();
        userPorts.put(user(1L, "sub-1", "EMP-1", UserStatus.PENDING, 1L));
        ManageProvisionedUserService service =
                new ManageProvisionedUserService(userPorts, userPorts, event -> {
                }, event -> {
                });

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.create(createCommand("sub-1", "EMP-2", true))
        );

        assertEquals(ErrorCode.USER_DUPLICATED_KEYCLOAK_SUB, exception.getErrorCode());
    }

    private CreateProvisionedUserCommand createCommand(
            String keycloakSub,
            String employeeNumber,
            boolean sourceActive
    ) {
        return new CreateProvisionedUserCommand(
                keycloakSub,
                employeeNumber,
                "사용자 " + employeeNumber,
                employeeNumber.toLowerCase() + "@example.com",
                "직원",
                UserRole.BRANCH_STAFF,
                TenancyType.BRANCH,
                "강남 지점",
                sourceActive
        );
    }

    private static User user(
            Long id,
            String keycloakSub,
            String employeeNumber,
            UserStatus status,
            Long version
    ) {
        return new User(
                id,
                keycloakSub,
                employeeNumber,
                "사용자 " + employeeNumber,
                employeeNumber.toLowerCase() + "@example.com",
                "직원",
                status,
                UserRole.BRANCH_STAFF,
                TenancyType.BRANCH,
                "강남 지점",
                version
        );
    }

    private static class InMemoryUserPorts implements LoadUserPort, SaveUserPort {

        private final Map<Long, User> users = new LinkedHashMap<>();
        private long nextId = 1L;

        private void put(User user) {
            users.put(user.id(), user);
            nextId = Math.max(nextId, user.id() + 1);
        }

        @Override
        public Optional<User> findByKeycloakSub(String keycloakSub) {
            return users.values().stream()
                    .filter(user -> user.keycloakSub().equals(keycloakSub))
                    .findFirst();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<User> findByEmployeeNumber(String employeeNumber) {
            return users.values().stream()
                    .filter(user -> user.employeeNumber().equals(employeeNumber))
                    .findFirst();
        }

        @Override
        public List<User> findAll(int offset, int count) {
            return users.values().stream()
                    .sorted(Comparator.comparing(User::id))
                    .skip(offset)
                    .limit(count)
                    .toList();
        }

        @Override
        public long countAll() {
            return users.size();
        }

        @Override
        public User save(User user) {
            Long id = user.id() == null ? nextId++ : user.id();
            long version = user.id() == null ? 1L : user.version() + 1;
            User saved = new User(
                    id,
                    user.keycloakSub(),
                    user.employeeNumber(),
                    user.displayName(),
                    user.email(),
                    user.position(),
                    user.status(),
                    user.role(),
                    user.tenancyType(),
                    user.tenancyName(),
                    version
            );
            users.put(id, saved);
            return saved;
        }
    }

    private static class RecordingEventPort implements RecordUserChangedEventPort {
        private UserChangedEvent recorded;

        @Override
        public void record(UserChangedEvent event) {
            this.recorded = event;
        }
    }

    private static class RecordingPublisher implements ApplicationEventPublisher {
        private Object published;

        @Override
        public void publishEvent(Object event) {
            this.published = event;
        }
    }
}
