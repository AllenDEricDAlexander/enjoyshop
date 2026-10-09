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
public class ProductStateBO {

    private Long id;

    private String status;

    private Boolean marketable;

    private Boolean deleted;

    private Long version;

    private Boolean hasActiveSku;

    private Boolean referencesValid;
}
