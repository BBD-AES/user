package com.bbd.user.application.port.in;

import com.bbd.user.application.model.UpdateUserStatusCommand;
import com.bbd.user.application.model.UserResult;

public interface UpdateUserStatusUseCase {

    UserResult updateStatus(UpdateUserStatusCommand command);
}