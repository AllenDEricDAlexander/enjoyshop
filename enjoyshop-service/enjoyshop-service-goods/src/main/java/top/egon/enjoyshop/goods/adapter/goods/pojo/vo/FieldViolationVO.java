package top.egon.enjoyshop.goods.adapter.goods.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One field-level violation inside the {@code 400} payload of {@code API-001} to {@code API-004}.
 *
 * <p>Built directly by the exception advice from the binding result; it never carries the rejected
 * value, only the field name, so the response cannot echo user input back (Spec §9.2.1 error body).</p>
 *
 * @param field      offending property name, matching the documented parameter table
 * @param constraint simple name of the constraint annotation that was violated
 * @param message    constraint template message, safe to display
 */
@Schema(description = "字段级校验错误")
public record FieldViolationVO(
        @Schema(description = "出错字段名") String field,
        @Schema(description = "命中的约束注解简名") String constraint,
        @Schema(description = "约束默认消息") String message) {
}
