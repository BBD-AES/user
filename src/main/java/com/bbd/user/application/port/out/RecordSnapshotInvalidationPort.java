package com.bbd.user.application.port.out;

import com.bbd.user.application.event.UserChangedEvent;

/*
 User 변경 트랜잭션 안에서 Redis Snapshot 무효화 작업을 기록하는 port.

 Redis 삭제 자체는 commit 이후에 수행하지만, 삭제해야 한다는 사실은
 사용자 변경과 같은 DB 트랜잭션에 먼저 저장해 복구 지점을 남긴다.
 */
public interface RecordSnapshotInvalidationPort {

    void record(UserChangedEvent event);
}
