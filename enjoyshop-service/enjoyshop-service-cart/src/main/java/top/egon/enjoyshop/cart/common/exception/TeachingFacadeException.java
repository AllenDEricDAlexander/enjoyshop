package top.egon.enjoyshop.cart.common.exception;

import java.io.Serial;

/** Teaching facade failure reported to an outbound caller. */
public class TeachingFacadeException extends BaseBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TeachingFacadeException(String code, String message) {
        super(code, message);
    }

    public TeachingFacadeException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
