package top.egon.enjoyshop.goods.adapter.goods.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "商品规格选项")
public record SpecOptionVO(
        @Schema(description = "规格名称", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "按存储顺序排列的可选值", requiredMode = Schema.RequiredMode.REQUIRED)
                List<String> options) {

    public SpecOptionVO {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
