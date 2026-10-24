package com.example.bankapproval.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.validation.ConstraintViolationException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @RestControllerAdvice 将异常稳定地转换成统一 JSON，前端无需猜测错误格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        HttpStatus status = "NOT_FOUND".equals(ex.getCode()) ? HttpStatus.NOT_FOUND :
                ("CONCURRENT_MODIFICATION".equals(ex.getCode()) ? HttpStatus.CONFLICT :
                        ("DATA_SCOPE_DENIED".equals(ex.getCode()) || "SEPARATION_OF_DUTIES".equals(ex.getCode())
                                ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST));
        return ResponseEntity.status(status).body(new ApiResponse<Void>(false, ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiResponse<Map<String, String>>(false, "VALIDATION_ERROR", "参数校验失败", errors));
    }

    // 查询对象绑定失败不会抛 MethodArgumentNotValidException，需单独处理，避免错误返回 500。
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBinding(BindException ex) {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiResponse<Map<String, String>>(false,
                "VALIDATION_ERROR", "查询参数校验失败", errors));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiResponse<Void>(false,
                "VALIDATION_ERROR", "参数格式或取值不正确", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<Void>(false, "FORBIDDEN", "无权执行此操作", null));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<Void>(false, "INTERNAL_ERROR", "系统繁忙，请联系管理员", null));
    }
}
