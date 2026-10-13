package top.egon.enjoyshop.cart.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.egon.cola.component.common.core.enums.ErrorStatus;

/**
 * Stable cart business codes. Codes are appended only and are never renumbered.
 */
@Getter
@AllArgsConstructor
public enum CartErrorStatus implements ErrorStatus {

    CART_FORBIDDEN(610501, "cart permission is denied"),
    CART_AUTHORIZATION_UNAVAILABLE(610502, "cart authorization is unavailable"),
    CART_REQUEST_INVALID(610503, "cart request parameters are invalid"),
    CART_SKU_NOT_SELLABLE(610504, "cart sku is not sellable"),
    CART_GOODS_UNAVAILABLE(610505, "cart goods detail is unavailable"),
    CART_QUANTITY_OVERFLOW(610506, "cart quantity overflow"),
    CART_VERSION_CONFLICT(610507, "cart version conflict"),
    CART_INTERNAL_ERROR(610599, "cart internal error");

    private final int code;

    private final String message;

    @Override
    public String getStatus() {
        return name();
    }
}
