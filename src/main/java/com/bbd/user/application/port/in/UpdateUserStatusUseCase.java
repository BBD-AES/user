package com.bbd.user.application.port.in;

import com.bbd.user.application.model.UpdateUserStatusCommand;
import com.bbd.user.application.model.UserSnapshotResult;

public interface UpdateUserStatusUseCase {

    UserSnapshotResult updateStatus(UpdateUserStatusCommand command);
}