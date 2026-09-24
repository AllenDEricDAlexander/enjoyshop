package top.egon.enjoyshop.goods.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the ASM-006 brand code segment and the {@code status = name()} contract.
 */
class GoodsErrorStatusTest {

    @Test
    void brand_error_codes_match_asm006() {
        assertThat(GoodsErrorStatus.BRAND_REQUEST_INVALID.getCode()).isEqualTo(610101);
        assertThat(GoodsErrorStatus.BRAND_NOT_FOUND.getCode()).isEqualTo(610102);
        assertThat(GoodsErrorStatus.BRAND_NAME_DUPLICATED.getCode()).isEqualTo(610103);
        assertThat(GoodsErrorStatus.BRAND_VERSION_CONFLICT.getCode()).isEqualTo(610104);
        assertThat(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED.getCode()).isEqualTo(610105);
        assertThat(GoodsErrorStatus.BRAND_INTERNAL_ERROR.getCode()).isEqualTo(610199);

        assertThat(GoodsErrorStatus.BRAND_REQUEST_INVALID.getMessage())
                .isEqualTo("request parameters are invalid");
        assertThat(GoodsErrorStatus.BRAND_NOT_FOUND.getMessage())
                .isEqualTo("brand does not exist or is not visible to current tenant");
        assertThat(GoodsErrorStatus.BRAND_NAME_DUPLICATED.getMessage())
                .isEqualTo("brand name already exists in current tenant");
        assertThat(GoodsErrorStatus.BRAND_VERSION_CONFLICT.getMessage())
                .isEqualTo("brand was updated by another operation");
        assertThat(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED.getMessage())
                .isEqualTo("identifiable operator is required for this operation");
        assertThat(GoodsErrorStatus.BRAND_INTERNAL_ERROR.getMessage())
                .isEqualTo("brand internal error");
    }

    @Test
    void status_is_enum_name() {
        assertThat(GoodsErrorStatus.BRAND_NAME_DUPLICATED.getStatus()).isEqualTo("BRAND_NAME_DUPLICATED");
        assertThat(GoodsErrorStatus.BRAND_REQUEST_INVALID.getStatus()).isEqualTo("BRAND_REQUEST_INVALID");
        assertThat(GoodsErrorStatus.BRAND_NOT_FOUND.getStatus()).isEqualTo("BRAND_NOT_FOUND");
        assertThat(GoodsErrorStatus.BRAND_VERSION_CONFLICT.getStatus()).isEqualTo("BRAND_VERSION_CONFLICT");
        assertThat(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED.getStatus()).isEqualTo("BRAND_OPERATOR_REQUIRED");
        assertThat(GoodsErrorStatus.BRAND_INTERNAL_ERROR.getStatus()).isEqualTo("BRAND_INTERNAL_ERROR");
    }
}
