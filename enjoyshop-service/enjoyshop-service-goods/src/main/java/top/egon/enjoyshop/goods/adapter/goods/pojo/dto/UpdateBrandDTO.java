package top.egon.enjoyshop.goods.adapter.goods.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload of {@code API-004}: a complete replacement of the editable columns.
 *
 * <p>Unlike {@link CreateBrandDTO} both {@code seq} and {@code version} are mandatory, because
 * whole-resource replacement has no "keep the previous value" semantics and the optimistic lock must
 * come from the caller's most recent read (Spec §9.2.4).</p>
 *
 * @param name    brand name, unique among active rows inside the tenant
 * @param image   brand image URL; explicit {@code null} clears the column
 * @param letter  single-letter index, normalised to uppercase by the adapter converter
 * @param seq     ordering weight, mandatory under replacement semantics
 * @param version optimistic-lock revision the caller last read
 */
@Schema(description = "整替换品牌请求")
public record UpdateBrandDTO(
        @Schema(description = "品牌名称，租户内有效行唯一") @NotBlank @Size(max = 120) String name,
        @Schema(description = "品牌图片 URL；显式 null 即清空") @Size(max = 500) String image,
        @Schema(description = "首字母，单字符 A-Z") @NotBlank @Pattern(regexp = "[A-Za-z]") String letter,
        @Schema(description = "排序权重，整替换必填") @NotNull @Min(0) @Max(999999) Integer seq,
        @Schema(description = "乐观锁版本，必须来自最近一次读取") @NotNull @PositiveOrZero Long version) {
}
