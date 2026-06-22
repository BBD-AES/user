package com.bbd.user.adapter.out.snapshot;

/*
 Redis Snapshot 무효화 작업 상태.

 PENDING:
 User DB 변경과 함께 저장됐지만 아직 Redis 삭제 완료가 확인되지 않은 상태.

 DONE:
 Redis DEL 성공을 확인한 상태.

 FAILED:
 Redis 삭제 실패가 설정된 재시도 상한에 도달해 자동 polling 대상에서 제외된 상태.
 */
public enum SnapshotInvalidationOutboxStatus {
    PENDING,
    DONE,
    FAILED
}
