package com.bbd.user.domain;

/*
 UserSnapshot에서 인가 가능 여부를 판단할 사용자 상태.

 이 값은 나중에 공통 인가 프레임워크가 Redis UserSnapshot을 읽을 때
 가장 먼저 검사하는 값이다.

 ACTIVE가 아니면 일반 업무 API 접근을 막는 방향으로 사용한다.
 */
public enum UserStatus {
    ACTIVE,
    PENDING,
    INACTIVE
}