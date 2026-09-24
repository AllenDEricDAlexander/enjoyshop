package top.egon.enjoyshop.goods.facade.user.utils;

import top.egon.enjoyshop.goods.common.exception.UserFacadeException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Facade-boundary guard; the null check itself belongs to the common validation facade. */
public final class UserFacadeAssert {
    private UserFacadeAssert() {
    }

    public static <T> T notNull(T value, String code, String message) {
        return ValidationUtils.requireNotNull(value, () -> new UserFacadeException(code, message));
    }
}
