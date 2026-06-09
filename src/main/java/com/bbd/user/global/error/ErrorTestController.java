package com.bbd.user.global.error;

import com.bbd.user.global.error.dto.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/error")
public class ErrorTestController {

    @GetMapping
    public ResponseEntity<Void> test1(){
        throw new ApiException(ErrorCode.VALIDATION_ERROR);
    }

}