package com.bbd.user.application.port.out;

import com.bbd.user.application.event.UserChangedEvent;

/*
 사용자 변경 이벤트를 영속화하기 위한 outbound port.

 application service는 Kafka에 직접 발행하지 않는다.
 User DB 변경과 같은 트랜잭션에 Outbox를 저장하기 위해 이 포트를 호출한다.

 현재 구현체는 UserOutboxPersistenceAdapter이며,
 Kafka 발행은 트랜잭션이 끝난 뒤 별도의 Publisher가 수행한다.
 */
public interface RecordUserChangedEventPort {

    void record(UserChangedEvent event);
}
