package top.egon.enjoyshop.goods.adapter.goods.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload of {@code API-003}.
 *
 * <p>Deliberately carries no derived field: {@code id}, {@code tenantId}, {@code version} and the
 * audit columns do not exist on this type at compile time, which is safer than ignoring them at
 * runtime (Spec §9.2.3 forbidden set).</p>
 *
 * @param name   brand name, unique among active rows inside the tenant
 * @param image  brand image URL, nullable; only length is constrained
 * @param letter single-letter index, normalised to uppercase by the adapter converter
 * @param seq    ordering weight; absent means the server supplies {@code 0}
 */
@Schema(description = "新增品牌请求")
public record CreateBrandRequest(
        @Schema(description = "品牌名称，租户内有效行唯一") @NotBlank @Size(max = 120) String name,
        @Schema(description = "品牌图片 URL，可空") @Size(max = 500) String image,
        @Schema(description = "首字母，单字符 A-Z") @NotBlank @Pattern(regexp = "[A-Za-z]") String letter,
        @Schema(description = "排序权重，未传补 0") @Min(0) @Max(999999) Integer seq) {
}
