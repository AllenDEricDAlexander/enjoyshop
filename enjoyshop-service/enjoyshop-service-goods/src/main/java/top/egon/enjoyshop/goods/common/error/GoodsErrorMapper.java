package top.egon.enjoyshop.goods.common.error;

import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

/**
 * Error boundary for the generated domain implementation: a missing row or a zero-row effect
 * becomes a {@link CommonException} carrying the stable brand business code.
 */
public final class GoodsErrorMapper {

    private GoodsErrorMapper() {
    }

    public static CommonException missing(Long id) {
        return new CommonException(GoodsErrorStatus.BRAND_NOT_FOUND);
    }

    public static CommonException zeroRows(String operation, Long id, Long version) {
        return switch (operation) {
            case "create" -> new CommonException(GoodsErrorStatus.BRAND_NAME_DUPLICATED);
            case "update" -> new CommonException(GoodsErrorStatus.BRAND_VERSION_CONFLICT);
            case "delete" -> new CommonException(GoodsErrorStatus.BRAND_NOT_FOUND);
            default -> new CommonException(GoodsErrorStatus.BRAND_INTERNAL_ERROR);
        };
    }
}
