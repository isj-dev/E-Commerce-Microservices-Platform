package com.isj.common.exception;

import com.isj.common.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 적용하려는 서비스에 common 서비스 종속성 추가 후 scanBasePackages = {"com.isj.common"} 설정 후 패키지 스캔을 완료하도록 하고
// @RestControllerAdvice 사용해서 Bean 으로 등록해야함
@RestControllerAdvice // AOP 기반 동작 (예외 발생시 spring이 컨트롤러로 응답 보내기전 가로채서 해당 핸들러로 전달)
public class GlobalExceptionHandler {

    // BusinessException 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode().getCode(), e.getMessage()));
    }

    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("C001", message));
    }
}
