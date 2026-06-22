package com.bbd.user.adapter.out.outbox;

import com.bbd.user.application.event.UserChangedEvent;
import com.bbd.user.application.port.out.RecordUserChangedEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/*
 RecordUserChangedEventPort의 Outbox 기반 구현체.

 application service가 만든 UserChangedEvent를 JSON으로 직렬화하고
 user_outbox 테이블에 PENDING 상태로 저장한다.

 이 adapter는 Kafka를 직접 호출하지 않는다.
 호출한 application service의 @Transactional 범위에 참여하므로
 User DB 변경과 Outbox 저장이 함께 commit 또는 rollback된다.
 */
@Component
@RequiredArgsConstructor
public class UserOutboxPersistenceAdapter implements RecordUserChangedEventPort {

    private final UserOutboxJpaRepository userOutboxJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void record(UserChangedEvent event) {
        try {
            // 타 서비스를 위한 것.
            // Kafka 소비 서비스가 동일한 event contract를 사용할 수 있도록 JSON payload로 저장한다.
            String payload = objectMapper.writeValueAsString(event);
            userOutboxJpaRepository.save(UserOutboxJpaEntity.pending(event, payload));
        } catch (Exception e) {
            // 직렬화나 Outbox 저장 준비가 실패하면 사용자 변경 트랜잭션도 rollback시킨다.
            throw new IllegalStateException("UserChanged 이벤트 직렬화에 실패했습니다.", e);
        }
    }
}
