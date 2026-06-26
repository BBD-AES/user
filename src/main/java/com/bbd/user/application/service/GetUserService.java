package com.bbd.user.application.service;

import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.in.GetUserUseCase;
import com.bbd.user.application.port.out.LoadUserPort;
import com.bbd.user.domain.User;
import com.bbd.user.global.error.ApiException;
import com.bbd.user.global.error.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 ERP 사용자 기본 정보 조회 서비스.
 관리자 변경 API와 SCIM provisioning API가 아닌, 화면 표시용 read-only 조회만 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserService implements GetUserUseCase {

    private final LoadUserPort loadUserPort;

    @Override
    public UserResult getById(Long userId) {
        User user = loadUserPort.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return UserResult.from(user);
    }
}
