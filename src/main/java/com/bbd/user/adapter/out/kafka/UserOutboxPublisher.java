package com.bbd.user.adapter.out.kafka;

import com.bbd.user.adapter.out.outbox.UserOutboxJpaEntity;
import com.bbd.user.adapter.out.outbox.UserOutboxJpaRepository;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
 user_outbox의 PENDING 이벤트를 Kafka로 발행하는 scheduled publisher.

 bbd.user.events.enabled=true일 때만 Bean이 생성된다.
 Kafka/Redis 인프라를 준비하기 전에는 false로 두어 기존 ERP 기능에 영향을 주지 않는다.

 처리 순서:

 1. PENDING Outbox를 batch 단위로 lock해서 조회
 2. keycloakSub를 message key로 Kafka 발행
 3. broker 응답 성공 시 PUBLISHED로 변경
 4. 실패 시 attempts와 lastError 기록, 재시도 상한 도달 시 FAILED로 격리

 Kafka send 성공 후 DB commit이 실패하면 같은 이벤트가 다시 발행될 수 있다.
 따라서 전체 전달 보장은 exactly-once가 아니라 at-least-once다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "enabled",
        havingValue = "true"
)
public class UserOutboxPublisher {

    private final UserOutboxJpaRepository userOutboxJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserEventProperties properties;

    /*
     fixedDelay는 이전 실행이 끝난 시점부터 다음 실행까지 기다리는 방식이다.
     한 인스턴스 안에서 이전 polling이 끝나기 전에 다음 polling이 겹치지 않는다.
     */
    @Scheduled(fixedDelayString = "${bbd.user.events.publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<UserOutboxJpaEntity> events =
                userOutboxJpaRepository.findPendingForPublish(properties.getOutboxBatchSize());

        for (UserOutboxJpaEntity event : events) {
            publish(event);
        }
    }

    /*
     send 결과를 timeout 안에 확인해서 Outbox 상태를 결정한다.
     비동기 send만 호출하고 즉시 PUBLISHED로 바꾸면 broker 실패를 놓칠 수 있으므로
     현재 구현은 broker 응답을 기다린다.
     */
    private void publish(UserOutboxJpaEntity event) {
        try {
            kafkaTemplate.send(
                            properties.getTopic(),
                            event.getEventKey(),
                            event.getPayload()
                    )
                    .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);

            event.markPublished(Instant.now());
        } catch (InterruptedException e) {
            // 종료 신호를 잃지 않도록 interrupt 상태를 복구한다.
            Thread.currentThread().interrupt();
            event.markFailed(e, properties.getOutboxMaxAttempts());
        } catch (Exception e) {
            event.markFailed(e, properties.getOutboxMaxAttempts());
        }
    }
}
