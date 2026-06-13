package com.bbd.user.application.port.out;

import com.bbd.user.domain.User;

/*
 application 계층이 User 도메인 변경을 저장하기 위해 사용하는 outbound port.

 현재 단계에서는 기존 사용자 수정만 지원한다.
 이후 SCIM 사용자 생성 기능을 구현할 때 신규 User 저장 정책을 확장할 수 있다.
 */
public interface SaveUserPort {

    User save(User user);
}
