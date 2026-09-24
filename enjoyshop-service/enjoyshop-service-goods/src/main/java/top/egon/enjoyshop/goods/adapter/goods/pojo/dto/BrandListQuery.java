package top.egon.enjoyshop.goods.adapter.goods.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Query-parameter carrier of {@code API-001}, bound as a model attribute.
 *
 * <p>Every component is nullable because absence is meaningful: no page parameter at all selects
 * full-catalog mode, and blank filters are normalised to "not supplied" by
 * {@code BrandAdapterConverter}. The cross-field {@code seqMin}/{@code seqMax} ordering is checked
 * in the controller, which is where the two-mode decision is taken (Spec §9.2.1).</p>
 *
 * @param name     name prefix filter
 * @param letter   exact single-letter filter
 * @param seqMin   inclusive lower bound of the ordering weight range
 * @param seqMax   inclusive upper bound of the ordering weight range
 * @param pageNo   page number starting at 1
 * @param pageSize page size, default 10 and hard maximum 500
 */
@Schema(description = "品牌集合查询参数；pageNo 与 pageSize 均缺失为全量模式（pageSize=500）")
public record BrandListQuery(
        @Schema(description = "名称前缀过滤") @Size(max = 120) String name,
        @Schema(description = "首字母精确过滤") @Pattern(regexp = "[A-Za-z]") String letter,
        @Schema(description = "seq 区间下界") @Min(0) Integer seqMin,
        @Schema(description = "seq 区间上界") @Min(0) Integer seqMax,
        @Schema(description = "页码，从 1 起") @Min(1) Integer pageNo,
        @Schema(description = "页大小，缺省 10、上限 500") @Min(1) Integer pageSize) {
}
