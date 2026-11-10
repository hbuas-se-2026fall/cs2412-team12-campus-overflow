package com.campusoverflow.shared.domain;

/**
 * 不变式违反：聚合内"必须永远成立"的规则被破坏（统一语言术语 #44）。
 *
 * <p>{@code code} 直接使用 Lab 3 聚合清单中的不变式编号（如 {@code INV-Q-04}），
 * 这样日志、接口响应、测试用例和设计文档可以互相追溯。</p>
 */
public class InvariantViolationException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvariantViolationException(String invariantCode, String message) {
        super(invariantCode, message);
    }

    public String invariantCode() {
        return code();
    }
}
