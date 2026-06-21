package com.bbd.user.adapter.out.kafka;

import com.bbd.user.application.port.out.PublishUserEventPort;
import com.bbd.user.config.UserEventProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "bbd.user.events",
        name = "enabled",
        havingValue = "true"
)
public class KafkaUserEventPublisher implements PublishUserEventPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserEventProperties properties;

    @Override
    public void publish(String eventKey, String payload)
            throws InterruptedException, ExecutionException, TimeoutException {
        kafkaTemplate.send(
                        properties.getTopic(),
                        eventKey,
                        payload
                )
                .get(properties.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
    }
}
