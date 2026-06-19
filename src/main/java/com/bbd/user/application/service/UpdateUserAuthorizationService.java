package com.bbd.user.application.service;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.in.UpdateUserAuthorizationUseCase;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.application.port.out.RecordUserChangedEventPort;
import com.bbd.user.application.port.out.SaveUserPort;
import com.bbd.user.domain.User;
import com.bbd.user.domain.UserRole;
import com.bbd.user.domain.UserStatus;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 ERP 사용자의 상태, 역할, 소속을 변경하는 application service.

 처리 순서:

 1. JWT sub로 변경 요청자 조회
 2. 요청자가 ACTIVE + ADMIN/HQ_MANAGER인지 검사
 3. 변경 대상 사용자 조회
 4. User 도메인 규칙으로 새 상태 생성
 5. users 테이블 수정
 6. 같은 DB 트랜잭션에 user_outbox 저장
 7. commit 후 Redis Snapshot 즉시 삭제용 Spring event 발행

 User 저장과 Outbox 저장 중 하나라도 실패하면 전체 트랜잭션이 rollback된다.
 따라서 DB만 바뀌고 이벤트가 사라지는 상태를 방지한다.

 Redis 즉시 삭제는 AFTER_COMMIT listener가 처리하고,
 Kafka 발행은 UserOutboxPublisher가 PENDING Outbox를 읽어서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class UpdateUserAuthorizationService implements UpdateUserAuthorizationUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final RecordUserChangedEventPort recordUserChangedEventPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public UserSnapshotResult updateAuthorization(UpdateUserAuthorizationCommand command) {
        User target = loadUserPort.findById(command.targetUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User changed = target.changeAuthorization(
                command.status(),
                command.role(),
                command.tenancyType(),
                command.tenancyName()
        );

        // saveAndFlush 과정에서 JPA @Version이 증가한 최종 User를 받는다.
        User saved = saveUserPort.save(changed);

        // INACTIVE 전환은 일반 권한 변경과 구분해서 event type을 기록한다.
        UserChangeType eventType = saved.status() == UserStatus.INACTIVE
                ? UserChangeType.USER_DEACTIVATED
                : UserChangeType.USER_AUTHORIZATION_CHANGED;

        // User 저장과 같은 @Transactional 범위에서 Outbox를 저장한다.
        UserChangedEvent event = UserChangedEvent.from(saved, eventType);
        recordUserChangedEventPort.record(event);

        /*
         Spring event는 현재 transaction 안에서 발행하지만 실제 Redis 삭제는
         AFTER_COMMIT listener가 DB commit 성공 이후에 수행한다.

         즉시 삭제가 실패해도 Outbox -> Kafka -> Consumer 경로가
         같은 Redis Snapshot key를 다시 삭제해 복구한다.
         */
        applicationEventPublisher.publishEvent(event);
        return UserSnapshotResult.from(saved);
    }
}