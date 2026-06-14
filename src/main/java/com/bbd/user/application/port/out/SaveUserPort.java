package com.bbd.user.application.port.out;

import com.bbd.user.domain.User;

/*
 application 계층이 User 도메인 변경을 저장하기 위해 사용하는 outbound port.

 관리자 API의 기존 사용자 수정과
 SCIM API의 신규 사용자 생성 및 정보 변경을 함께 지원한다.
 */
public interface SaveUserPort {

    User save(User user);
}
