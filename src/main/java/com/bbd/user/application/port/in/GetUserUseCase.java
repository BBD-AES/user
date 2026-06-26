package com.bbd.user.application.port.in;

import com.bbd.user.application.model.UserResult;

/*
 ERP 화면에서 사용자 기본 정보를 조회하는 read-only 유스케이스.
 권한/status 변경 같은 관리자 기능과 분리해서 일반 로그인 사용자도 사용할 수 있다.
 */
public interface GetUserUseCase {

    UserResult getById(Long userId);
}
