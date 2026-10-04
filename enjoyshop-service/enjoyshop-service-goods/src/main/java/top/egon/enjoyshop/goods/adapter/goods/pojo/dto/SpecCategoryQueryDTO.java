package top.egon.enjoyshop.goods.adapter.goods.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.ValidCategoryName;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class SpecCategoryQueryDTO {

    @ValidCategoryName(required = true)
    private String categoryName;
}
