package com.campusoverflow.bootstrap.interfaces.error;

import com.campusoverflow.identity.application.exception.DuplicateEmailException;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.domain.InvariantViolationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * 全局异常处理器（Lab 4 · Step 3）：把领域异常翻译成统一错误响应。
 *
 * <p>分工原则：领域层只表达"哪条规则被违反"（code），HTTP 语义（400/404/409/422/500）全部在这里决定，
 * 领域代码不 import 任何 Spring Web 类型。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 不变式违反：请求能到达业务，但业务规则不允许 → 400。 */
    @ExceptionHandler(InvariantViolationException.class)
    public ResponseEntity<ErrorResponse> handleInvariant(InvariantViolationException ex, HttpServletRequest request) {
        log.warn("invariant violated: code={}, path={}, message={}", ex.code(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.code(), ex.getMessage(), request, List.of());
    }

    /** 邮箱重复：语义上更像冲突 → 409。 */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        log.warn("duplicate email: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.code(), ex.getMessage(), request, List.of());
    }

    /** 聚合不存在 → 404。 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("entity not found: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.code(), ex.getMessage(), request, List.of());
    }

    /** 其余领域异常：业务上无法处理的请求 → 422。 */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex, HttpServletRequest request) {
        log.warn("domain exception: code={}, path={}, message={}", ex.code(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage(), request, List.of());
    }

    /** Bean Validation 失败：一次返回所有字段错误，避免前端改一个字段提一次。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.Detail(error.getField(), error.getDefaultMessage()))
                .toList();
        String message = details.isEmpty() ? "请求参数校验失败" : details.get(0).message();
        log.warn("validation failed: path={}, details={}", request.getRequestURI(), details.size());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request, details);
    }

    /** URL / Query 参数校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.Detail> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.Detail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求参数校验失败", request, details);
    }

    /** JSON 解析失败 / 字段类型不对。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        log.warn("malformed request body: path={}", request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "请求体不是合法的 JSON 或字段类型不匹配", request, List.of());
    }

    /** 路径变量类型不匹配（如 UUID 写错）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                "参数 " + ex.getName() + " 格式不正确", request, List.of());
    }

    /** 领域外抛出的非法参数（例如 UUID 解析失败）。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        log.warn("illegal argument: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request, List.of());
    }

    /** 兜底：不把堆栈和内部细节暴露给调用方，但必须留下可排查的日志。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("unexpected error: path={}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务器开小差了，请稍后重试或联系管理员", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest request, List<ErrorResponse.Detail> details) {
        String traceId = MDC.get("traceId");
        ErrorResponse body = ErrorResponse.of(code, message, traceId, Instant.now(), request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
