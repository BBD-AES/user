package com.bbd.user.application.port.in;

import com.bbd.user.application.model.UpdateUserAuthorizationCommand;
import com.bbd.user.application.model.UserSnapshotResult;

/*
 사용자 상태, 역할, 소속을 변경하는 inbound port.

 Web Controller는 구현 클래스나 JPA를 직접 알지 않고 이 인터페이스만 호출한다.
 실제 구현은 UpdateUserAuthorizationService가 담당한다.
 */
public interface UpdateUserAuthorizationUseCase {

    UserSnapshotResult updateAuthorization(UpdateUserAuthorizationCommand command);
}
