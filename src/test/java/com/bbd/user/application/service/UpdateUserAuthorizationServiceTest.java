package com.bbd.user.application.service;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.RecordUserChangedEventPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.TenancyType;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 UpdateUserAuthorizationService의 순수 application 규칙을 검증하는 단위 테스트.

 Spring Context나 실제 DB를 사용하지 않고 stub port를 주입한다.

 검증 대상:
 - target 사용자의 status, role, tenancy를 변경하는지
 - 저장 후 증가한 version이 event에도 들어가는지
 - 같은 event가 Outbox와 AFTER_COMMIT 처리 양쪽에 전달되는지

 호출자 권한 검사는 @RequireRole에서 처리하므로 이 테스트의 검증 대상이 아니다.
 */
class UpdateUserAuthorizationServiceTest {

    @Test
    void changeAuthorizationAndRecordEvent() {
        User target = user(2L, "target-sub", UserStatus.PENDING, UserRole.BRANCH_STAFF, 3L);
        StubUserPorts userPorts = new StubUserPorts(target);
        StubEventPort eventPort = new StubEventPort();
        StubApplicationEventPublisher applicationEventPublisher = new StubApplicationEventPublisher();

        UpdateUserAuthorizationService service =
                new UpdateUserAuthorizationService(
                        userPorts,
                        userPorts,
                        eventPort,
                        applicationEventPublisher
                );

        UserResult result = service.updateAuthorization(
                new UpdateUserAuthorizationCommand(
                        target.id(),
                        UserStatus.ACTIVE,
                        UserRole.BRANCH_MANAGER,
                        TenancyType.BRANCH,
                        "강남 지점"
                )
        );

        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(UserRole.BRANCH_MANAGER, result.role());
        assertEquals(TenancyType.BRANCH, result.tenancyType());
        assertEquals("강남 지점", result.tenancyName());
        assertEquals(4L, result.version());
        assertEquals(result.keycloakSub(), eventPort.recorded.keycloakSub());
        assertEquals(result.version(), eventPort.recorded.version());
        assertEquals(eventPort.recorded, applicationEventPublisher.published);
    }

    @Test
    void changeAuthorizationUpdatesRoleAndTenancy() {
        User target = user(2L, "target-sub", UserStatus.PENDING, UserRole.BRANCH_STAFF, 1L);
        StubUserPorts userPorts = new StubUserPorts(target);

        UpdateUserAuthorizationService service =
                new UpdateUserAuthorizationService(userPorts, userPorts, event -> {
                }, event -> {
                });

        UserResult result = service.updateAuthorization(
                new UpdateUserAuthorizationCommand(
                        target.id(),
                        UserStatus.ACTIVE,
                        UserRole.HQ_STAFF,
                        TenancyType.HQ,
                        "본사"
                )
        );

        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(UserRole.HQ_STAFF, result.role());
        assertEquals(TenancyType.HQ, result.tenancyType());
        assertEquals("본사", result.tenancyName());
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
                "사용자 " + id,
                "user" + id + "@example.com",
                "직원",
                status,
                role,
                role == UserRole.ADMIN || role.name().startsWith("HQ")
                        ? TenancyType.HQ
                        : TenancyType.BRANCH,
                role == UserRole.ADMIN || role.name().startsWith("HQ")
                        ? "본사"
                        : "지점",
                version
        );
    }

    private static class StubUserPorts implements LoadUserPort, SaveUserPort {

        private User target;

        private StubUserPorts(User target) {
            this.target = target;
        }

        @Override
        public Optional<User> findByKeycloakSub(String keycloakSub) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return target.id().equals(userId)
                    ? Optional.of(target)
                    : Optional.empty();
        }

        @Override
        public User save(User user) {
            // 실제 JPA @Version이 update 성공 시 1 증가시키는 동작을 단위 테스트에서 흉내 낸다.
            this.target = new User(
                    user.id(),
                    user.keycloakSub(),
                    user.employeeNumber(),
                    user.displayName(),
                    user.email(),
                    user.position(),
                    user.status(),
                    user.role(),
                    user.tenancyType(),
                    user.tenancyName(),
                    user.version() + 1
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