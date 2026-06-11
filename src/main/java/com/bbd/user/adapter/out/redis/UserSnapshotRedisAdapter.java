package com.bbd.user.adapter.out.redis;

import com.bbd.user.application.model.UserSnapshotResult;
import com.bbd.user.application.port.out.CacheUserSnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class UserSnapshotRedisAdapter implements CacheUserSnapshotPort {

    private static final String KEY_PREFIX = "user:snapshot:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${user.snapshot.cache.ttl-seconds}")
    private long ttlSeconds;

    @Override
    public void save(UserSnapshotResult snapshot) {
        try {
            String key = KEY_PREFIX + snapshot.keycloakSub();
            String value = objectMapper.writeValueAsString(snapshot);

            redisTemplate.opsForValue()
                    .set(key, value, Duration.ofSeconds(ttlSeconds));

        } catch (JacksonException e) {
            throw new IllegalStateException("UserSnapshot Redis 직렬화에 실패했습니다.", e);
        }
    }
}