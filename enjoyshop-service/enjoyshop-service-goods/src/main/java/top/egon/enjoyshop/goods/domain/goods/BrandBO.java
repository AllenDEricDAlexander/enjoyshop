package top.egon.enjoyshop.goods.domain.goods;

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
public class BrandBO {

    private Long id;

    private Long version;

    private String name;

    private String image;

    private String letter;

    private Integer seq;
}
