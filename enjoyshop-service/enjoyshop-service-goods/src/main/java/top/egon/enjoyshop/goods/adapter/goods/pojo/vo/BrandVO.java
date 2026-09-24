package top.egon.enjoyshop.goods.adapter.goods.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The single outbound representation of a brand, shared by {@code API-001} to {@code API-004}.
 *
 * <p>{@code tenantId}, {@code deletedAt}, {@code createUserId} and {@code updateUserId} are absent
 * on purpose: an unknown identifier and a cross-tenant one must stay indistinguishable, so the
 * projection must not leak any scoping column (Spec {@code REQ-010}). The audit timestamps are
 * excluded in this iteration as recorded by {@code PLAN-CLAR-011}.</p>
 *
 * @param id      snowflake brand identifier, emitted as a JSON integer
 * @param name    brand name as normalised on the way in
 * @param image   image URL, nullable
 * @param letter  always an uppercase single character
 * @param seq     ordering weight, larger sorts first
 * @param version optimistic-lock revision the caller must echo back on writes
 */
@Schema(description = "品牌表示；version 为写操作必须回传的乐观锁身份")
public record BrandVO(
        @Schema(description = "品牌 ID（雪花）") Long id,
        @Schema(description = "品牌名称") String name,
        @Schema(description = "图片 URL，可空") String image,
        @Schema(description = "大写首字母") String letter,
        @Schema(description = "排序权重") Integer seq,
        @Schema(description = "乐观锁版本") Long version) {
}
