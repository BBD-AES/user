package com.bbd.user.adapter.in.web;

import com.bbd.user.adapter.in.web.response.UserDirectoryResponse;
import com.bbd.user.application.model.UserResult;
import com.bbd.user.application.port.in.GetUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 ERP 프런트에서 사원 기본 정보를 조회하는 read-only API.
 /api/** 보안 체인에 의해 로그인은 필요하지만, ADMIN 역할은 요구하지 않는다.
 */
@Tag(name = "1. ERP User Directory Controller")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final GetUserUseCase getUserUseCase;

    @Operation(summary = "사원 단건 조회 API")
    @GetMapping("/{userId}")
    public UserDirectoryResponse getUser(@PathVariable Long userId) {
        UserResult result = getUserUseCase.getById(userId);
        return UserDirectoryResponse.from(result);
    }
}
