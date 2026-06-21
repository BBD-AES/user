package com.bbd.user.adapter.out.outbox;

/*
 Outbox event의 발행 상태.

 PENDING:
 User DB 변경과 함께 저장됐지만 아직 Kafka broker의 응답을 받지 못한 상태.

 PUBLISHED:
 Kafka send 결과를 성공으로 확인한 상태.

 FAILED:
 Kafka 발행 실패가 설정된 재시도 상한에 도달해 자동 polling 대상에서 제외된 상태.
 */
public enum UserOutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
