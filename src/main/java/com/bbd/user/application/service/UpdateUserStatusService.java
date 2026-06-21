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
        User target = loadUserPort.findById(command.targetUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User saved = saveUserPort.save(target.changeStatus(command.status()));

        UserChangeType eventType = saved.status() == UserStatus.INACTIVE
                ? UserChangeType.USER_DEACTIVATED
                : UserChangeType.USER_AUTHORIZATION_CHANGED;

        UserChangedEvent event = UserChangedEvent.from(saved, eventType);
        recordUserChangedEventPort.record(event);
        applicationEventPublisher.publishEvent(event);

        return UserResult.from(saved);
    }
}