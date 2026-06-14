package com.bbd.user.application.event;

/*
 User Service에서 발생할 수 있는 사용자 변경 이벤트 종류.

 관리자 API는 USER_AUTHORIZATION_CHANGED와 USER_DEACTIVATED를 사용한다.
 SCIM API는 생성, 프로필 변경, 비활성화에 맞는 이벤트를 사용한다.

 Kafka Consumer는 이 값을 통해 변경의 의미를 구분할 수 있지만,
 현재 Redis Snapshot Consumer는 모든 변경에서 동일하게 cache를 삭제한다.
 */
public enum UserChangeType {
    USER_CREATED,
    USER_PROFILE_CHANGED,
    USER_AUTHORIZATION_CHANGED,
    USER_DEACTIVATED
}
