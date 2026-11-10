package com.campusoverflow.shared.domain;

/**
 * 领域异常基类：所有可预期的业务规则失败都带一个稳定的 {@code code}，
 * 由接口层的全局异常处理器翻译成统一错误响应，前端按 code 做分支。
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
