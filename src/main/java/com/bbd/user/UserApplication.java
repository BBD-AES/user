package com.bbd.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 @ConfigurationPropertiesScan: application.yml의 bbd.user.events.* 값을
 UserEventProperties 같은 설정 객체에 바인딩한다.

 @EnableScheduling:
 Outbox에 저장된 사용자 변경 이벤트를 Kafka로 발행한다.
 Kafka Consumer는 Redis Snapshot을 다시 삭제해
 즉시 삭제 실패를 복구하고 변경 이벤트 전달을 보장한다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class UserApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}

}
