package com.bbd.user.application.service;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.model.*;
import com.bbd.user.application.port.in.ManageProvisionedUserUseCase;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.RecordUserChangedEventPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserStatus;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/*
 midPoint가 전달한 ERP 대상 사용자의 생성, reconciliation, 비활성화를 처리한다.

 모든 변경은 users 저장과 user_outbox 저장을 같은 트랜잭션에서 처리한다.
 commit 이후 Redis 즉시 삭제와 Kafka 복구 경로는 Phase 6 구현을 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class ManageProvisionedUserService implements ManageProvisionedUserUseCase {

    private static final int MAX_PAGE_SIZE = 200;

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final RecordUserChangedEventPort recordUserChangedEventPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public ProvisionedUserResult create(CreateProvisionedUserCommand command) {
        validateCreate(command);
        rejectDuplicate(command.keycloakSub(), command.employeeNumber(), command.username(), null);

        User created = saveUserPort.save(User.pendingProvisioning(
                command.keycloakSub(),
                command.employeeNumber(),
                command.username(),
                command.displayName(),
                command.email(),
                command.position(),
                command.role(),
                command.tenancyType(),
                command.tenancyName(),
                command.sourceActive()
        ));

        publish(created, UserChangeType.USER_CREATED);
        return ProvisionedUserResult.from(created);
    }

    @Override
    @Transactional
    public ProvisionedUserResult update(UpdateProvisionedUserCommand command) {
        User current = loadUserPort.findById(command.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        rejectDuplicate(
                current.getKeycloakSub(),
                command.employeeNumber(),
                command.username(),
                current.getId()
        );

        User saved = saveUserPort.save(current.updateProvisioning(
                command.employeeNumber(),
                command.username(),
                command.displayName(),
                command.email(),
                command.position(),
                command.role(),
                command.tenancyType(),
                command.tenancyName(),
                command.sourceActive()
        ));

        UserChangeType eventType = saved.getStatus() == UserStatus.INACTIVE
                ? UserChangeType.USER_DEACTIVATED
                : UserChangeType.USER_PROFILE_CHANGED;

        publish(saved, eventType);
        return ProvisionedUserResult.from(saved);
    }

    @Override
    @Transactional
    public ProvisionedUserResult deactivate(Long userId) {
        User current = loadUserPort.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User saved = saveUserPort.save(current.deactivate());
        publish(saved, UserChangeType.USER_DEACTIVATED);
        return ProvisionedUserResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisionedUserResult getById(Long userId) {
        return loadUserPort.findById(userId)
                .map(ProvisionedUserResult::from)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisionedUserSearchResult search(SearchProvisionedUsersCommand command) {
        int startIndex = Math.max(command.startIndex(), 1);
        int count = Math.min(Math.max(command.count(), 1), MAX_PAGE_SIZE);

        if (command.field() != null) {
            List<ProvisionedUserResult> users = findExact(command.field(), command.value())
                    .map(ProvisionedUserResult::from)
                    .stream()
                    .toList();

            return new ProvisionedUserSearchResult(users, users.size(), startIndex);
        }

        int offset = startIndex - 1;
        List<ProvisionedUserResult> users = loadUserPort.findAll(offset, count)
                .stream()
                .map(ProvisionedUserResult::from)
                .toList();

        return new ProvisionedUserSearchResult(users, loadUserPort.countAll(), startIndex);
    }

    private Optional<User> findExact(ProvisionedUserSearchField field, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return switch (field) {
            case KEYCLOAK_SUB -> loadUserPort.findByKeycloakSub(value);
            case EMPLOYEE_NUMBER -> loadUserPort.findByEmployeeNumber(value);
            case USERNAME -> loadUserPort.findByUsername(value);
        };
    }

    private void validateCreate(CreateProvisionedUserCommand command) {
        if (command.keycloakSub() == null || command.keycloakSub().isBlank()) {
            throw new ApiException(ErrorCode.USER_INVALID_KEYCLOAK_SUB);
        }
        if (command.employeeNumber() == null || command.employeeNumber().isBlank()) {
            throw new ApiException(ErrorCode.USER_INVALID_EMPLOYEE_NUMBER);
        }
        if (command.username() == null || command.username().isBlank()) {
            throw new ApiException(ErrorCode.USER_INVALID_USERNAME);
        }
        if (command.role() == null || command.tenancyType() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void rejectDuplicate(
            String keycloakSub,
            String employeeNumber,
            String username,
            Long currentUserId
    ) {
        rejectIfOtherUser(
                loadUserPort.findByKeycloakSub(keycloakSub),
                currentUserId,
                ErrorCode.USER_DUPLICATED_KEYCLOAK_SUB
        );

        if (employeeNumber != null) {
            rejectIfOtherUser(
                    loadUserPort.findByEmployeeNumber(employeeNumber),
                    currentUserId,
                    ErrorCode.USER_DUPLICATED_EMPLOYEE_NUMBER
            );
        }

        if (username != null) {
            rejectIfOtherUser(
                    loadUserPort.findByUsername(username),
                    currentUserId,
                    ErrorCode.USER_DUPLICATED_USERNAME
            );
        }
    }

    private void rejectIfOtherUser(Optional<User> existing, Long currentUserId, ErrorCode errorCode) {
        if (existing.isPresent() && !existing.get().getId().equals(currentUserId)) {
            throw new ApiException(errorCode);
        }
    }

    private void publish(User user, UserChangeType eventType) {
        UserChangedEvent event = UserChangedEvent.from(user, eventType);
        recordUserChangedEventPort.record(event);
        applicationEventPublisher.publishEvent(event);
    }
}
