package com.bbd.user.application.service;

import com.bbd.user.application.event.UserChangeType;
import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.model.UpdateUserStatusCommand;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.in.UpdateUserStatusUseCase;
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

/*
 관리자 API에서 사용자의 상태를 변경하는 application service.

 사용자를 조회한 뒤 도메인 모델의 changeStatus()로 상태를 변경하고,
 변경 결과를 저장한다.

 저장 이후에는 UserSnapshot 갱신/삭제와 Kafka Outbox 복구 흐름에 사용할
 UserChangedEvent를 기록하고 애플리케이션 이벤트로 발행한다.
 */
@Service
@RequiredArgsConstructor
public class UpdateUserStatusService implements UpdateUserStatusUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final RecordUserChangedEventPort recordUserChangedEventPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public UserResult updateStatus(UpdateUserStatusCommand command) {
        /*
         path variable로 전달된 targetUserId 기준으로 변경 대상 사용자를 조회한다.
         대상 사용자가 없으면 USER_NOT_FOUND 예외를 발생시킨다.
         */
        User target = loadUserPort.findById(command.targetUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        /*
         도메인 모델에 상태 변경을 위임한 뒤 저장한다.
         changeStatus()는 변경된 User 도메인 객체를 반환한다.
         */
        User saved = saveUserPort.save(target.changeStatus(command.status()));

        /*
         비활성화 상태로 변경된 경우에는 USER_DEACTIVATED 이벤트로 구분한다.
         그 외 상태 변경은 권한/인가 정보 변경 이벤트로 처리한다.
         */
        UserChangeType eventType = saved.status() == UserStatus.INACTIVE
                ? UserChangeType.USER_DEACTIVATED
                : UserChangeType.USER_AUTHORIZATION_CHANGED;

        /*
         변경된 사용자 정보를 기반으로 이벤트를 생성한다.
         record()는 Outbox 등에 이벤트를 기록해 장애 시 재처리할 수 있게 한다.
         publishEvent()는 트랜잭션 이후 Redis Snapshot 갱신/삭제 같은 후속 처리를 트리거한다.
         */
        UserChangedEvent event = UserChangedEvent.from(saved, eventType);
        recordUserChangedEventPort.record(event);
        applicationEventPublisher.publishEvent(event);

        /*
         저장된 도메인 User를 application 응답 모델로 변환해 반환한다.
         */
        return UserResult.from(saved);
    }
}