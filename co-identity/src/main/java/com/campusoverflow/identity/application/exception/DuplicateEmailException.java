package com.campusoverflow.identity.application.exception;

import com.campusoverflow.identity.domain.model.Email;
import com.campusoverflow.identity.domain.model.UserInvariants;
import com.campusoverflow.shared.domain.DomainException;

/**
 * 邮箱已注册（{@code INV-U-01}）：应用层在写库前先查一次，数据库唯一索引作为并发兜底。
 */
public class DuplicateEmailException extends DomainException {

    private static final long serialVersionUID = 1L;

    public DuplicateEmailException(Email email) {
        super(UserInvariants.EMAIL_UNIQUE, "该邮箱已注册：" + email.value());
    }
}
