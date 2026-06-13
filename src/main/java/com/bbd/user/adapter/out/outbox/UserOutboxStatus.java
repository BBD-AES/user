package com.bbd.user.adapter.out.outbox;

/*
 Outbox event의 발행 상태.

 PENDING:
 User DB 변경과 함께 저장됐지만 아직 Kafka broker의 응답을 받지 못한 상태.

 PUBLISHED:
 Kafka send 결과를 성공으로 확인한 상태.

 발행 실패는 별도 FAILED 상태로 고정하지 않는다.
 PENDING을 유지하면서 attempts와 lastError를 기록해 다음 polling에서 재시도한다.
 */
public enum UserOutboxStatus {
    PENDING,
    PUBLISHED
}
