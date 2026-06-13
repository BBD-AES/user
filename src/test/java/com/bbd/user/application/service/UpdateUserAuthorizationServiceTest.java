package com.bbd.user.application.service;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserSnapshotResult;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 UpdateUserAuthorizationService의 순수 application 규칙을 검증하는 단위 테스트.

 Spring Context나 실제 DB를 사용하지 않고 stub port를 주입한다.

 검증 대상:
 - ACTIVE + HQ_MANAGER가 권한을 변경할 수 있는지
 - 저장 후 증가한 version이 event에도 들어가는지
 - 같은 event가 Outbox와 AFTER_COMMIT 처리 양쪽에 전달되는지
 - HQ_MANAGER가 아닌 사용자를 차단하는지
 */
class UpdateUserAuthorizationServiceTest {

    @Test
    void activeHqManagerCanChangeAuthorizationAndRecordEvent() {
        User actor = user(1L, "manager-sub", UserStatus.ACTIVE, UserRole.HQ_MANAGER, 1L);
        User target = user(2L, "target-sub", UserStatus.PENDING, UserRole.BRANCH_STAFF, 3L);
        StubUserPorts userPorts = new StubUserPorts(actor, target);
        StubEventPort eventPort = new StubEventPort();
        StubApplicationEventPublisher applicationEventPublisher = new StubApplicationEventPublisher();

        UpdateUserAuthorizationService service =
                new UpdateUserAuthorizationService(
                        userPorts,
                        userPorts,
                        eventPort,
                        applicationEventPublisher
                );

        UserSnapshotResult result = service.updateAuthorization(
                new UpdateUserAuthorizationCommand(
                        actor.getKeycloakSub(),
                        target.getId(),
                        UserStatus.ACTIVE,
                        UserRole.BRANCH_MANAGER,
                        TenancyType.BRANCH,
                        "강남 지점"
                )
        );

        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(UserRole.BRANCH_MANAGER, result.role());
        assertEquals(4L, result.version());
        assertEquals(result.keycloakSub(), eventPort.recorded.keycloakSub());
        assertEquals(result.version(), eventPort.recorded.version());
        assertEquals(eventPort.recorded, applicationEventPublisher.published);
    }

    @Test
    void nonManagerCannotChangeAuthorization() {
        User actor = user(1L, "staff-sub", UserStatus.ACTIVE, UserRole.HQ_STAFF, 1L);
        User target = user(2L, "target-sub", UserStatus.PENDING, UserRole.BRANCH_STAFF, 1L);
        StubUserPorts userPorts = new StubUserPorts(actor, target);

        UpdateUserAuthorizationService service =
                new UpdateUserAuthorizationService(userPorts, userPorts, event -> {
                }, event -> {
                });

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.updateAuthorization(
                        new UpdateUserAuthorizationCommand(
                                actor.getKeycloakSub(),
                                target.getId(),
                                UserStatus.ACTIVE,
                                UserRole.BRANCH_STAFF,
                                TenancyType.BRANCH,
                                "강남 지점"
                        )
                )
        );

        assertEquals(ErrorCode.AUTH_FORBIDDEN, exception.getErrorCode());
    }

    private static User user(
            Long id,
            String keycloakSub,
            UserStatus status,
            UserRole role,
            Long version
    ) {
        return new User(
                id,
                keycloakSub,
                "EMP-" + id,
                "user-" + id,
                "사용자 " + id,
                "user" + id + "@example.com",
                "직원",
                status,
                role,
                role.name().startsWith("HQ") ? TenancyType.HQ : TenancyType.BRANCH,
                role.name().startsWith("HQ") ? "본사" : "지점",
                version
        );
    }

    private static class StubUserPorts implements LoadUserPort, SaveUserPort {

        private final User actor;
        private User target;

        private StubUserPorts(User actor, User target) {
            this.actor = actor;
            this.target = target;
        }

        @Override
        public Optional<User> findByKeycloakSub(String keycloakSub) {
            return actor.getKeycloakSub().equals(keycloakSub)
                    ? Optional.of(actor)
                    : Optional.empty();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return target.getId().equals(userId)
                    ? Optional.of(target)
                    : Optional.empty();
        }

        @Override
        public User save(User user) {
            // 실제 JPA @Version이 update 성공 시 1 증가시키는 동작을 단위 테스트에서 흉내 낸다.
            this.target = new User(
                    user.getId(),
                    user.getKeycloakSub(),
                    user.getEmployeeNumber(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getPosition(),
                    user.getStatus(),
                    user.getRole(),
                    user.getTenancyType(),
                    user.getTenancyName(),
                    user.getVersion() + 1
            );
            return target;
        }
    }

    private static class StubEventPort implements RecordUserChangedEventPort {

        // application service가 기록한 event를 assertion에서 확인하기 위한 test double.
        private UserChangedEvent recorded;

        @Override
        public void record(UserChangedEvent event) {
            this.recorded = event;
        }
    }

    private static class StubApplicationEventPublisher implements ApplicationEventPublisher {

        // Outbox에 저장한 event와 같은 event가 AFTER_COMMIT 처리용으로 발행되는지 확인한다.
        private Object published;

        @Override
        public void publishEvent(Object event) {
            this.published = event;
        }
    }
}
