package com.bbd.user.adapter.in.web.request;

import com.bbd.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull UserStatus status
) {
}