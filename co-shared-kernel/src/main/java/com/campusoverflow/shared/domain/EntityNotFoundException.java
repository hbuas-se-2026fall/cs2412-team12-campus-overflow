package com.campusoverflow.shared.domain;

/**
 * 聚合/实体不存在：接口层统一翻译成 404。
 */
public class EntityNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "NOT_FOUND";

    public EntityNotFoundException(String message) {
        super(CODE, message);
    }

    public static EntityNotFoundException of(String aggregateName, Object id) {
        return new EntityNotFoundException(aggregateName + " 不存在：" + id);
    }
}
