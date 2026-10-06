package com.bluecrystal.common.web;

import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.CommonException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：业务异常返回对应 HTTP 状态码，参数校验失败返回 400，其余统一 500。
 */
@Slf4j
@RestControllerAdvice
public class CommonExceptionAdvice {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<R<Void>> handleCommonException(CommonException e) {
        log.warn("业务异常：code={}, msg={}", e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.resolve(e.getCode());
        return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                .body(R.error(e.getCode(), e.getMessage()));
    }

    /** @RequestBody + @Valid 校验失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    /** 表单/查询参数绑定校验失败。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    /** 方法参数上的 @Validated 校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error(500, "服务器繁忙，请稍后再试"));
    }

    private ResponseEntity<R<Void>> badRequest(String msg) {
        String message = msg == null || msg.isBlank() ? "请求参数不合法" : msg;
        return ResponseEntity.badRequest().body(R.error(400, message));
    }
}
