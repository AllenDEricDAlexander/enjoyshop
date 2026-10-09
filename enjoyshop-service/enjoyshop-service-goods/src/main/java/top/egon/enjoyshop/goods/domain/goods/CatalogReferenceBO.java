package top.egon.enjoyshop.goods.domain.goods;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class CatalogReferenceBO {

    private Long brandId;

    private Long category1Id;

    private Long category2Id;

    private Long category3Id;

    private Long templateId;

    private String brandName;

    private String categoryName;

    @Builder.Default
    private List<SpecBO> orderedSpecs = List.of();

    @Builder.Default
    private Map<String,List<String>> paraOptions = Map.of();
}
