package com.bbd.user.application.port.out;

import com.bbd.user.application.model.UserSnapshotResult;

public interface CacheUserSnapshotPort {

    void save(UserSnapshotResult snapshot);
}