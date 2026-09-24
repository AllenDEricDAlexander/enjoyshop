package top.egon.enjoyshop.goods.adapter.goods.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * {@code data} payload of a {@code 610101} failure.
 *
 * <p>The envelope fields stay untouched and the details ride in {@code data}, which is the only
 * slot {@link top.egon.cola.component.common.core.pojo.ResultRecord} offers (Spec §9.2.1). The
 * compact constructor keeps the list non-null and immutable, so a client never has to branch on a
 * missing {@code fieldErrors} key.</p>
 *
 * @param fieldErrors per-field violations, never null and possibly empty
 */
@Schema(description = "字段错误集合")
public record FieldErrorsVO(
        @Schema(description = "逐项字段错误，永不 null") List<FieldViolationVO> fieldErrors) {

    public FieldErrorsVO {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
