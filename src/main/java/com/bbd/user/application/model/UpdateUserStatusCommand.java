package com.bbd.user.application.model;

import com.bbd.user.domain.UserStatus;

public record UpdateUserStatusCommand(
        Long targetUserId,
        UserStatus status
) {
}