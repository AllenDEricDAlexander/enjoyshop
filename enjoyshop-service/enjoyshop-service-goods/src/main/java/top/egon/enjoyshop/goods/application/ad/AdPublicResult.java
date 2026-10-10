package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AdPublicResult {

    private String id;

    private String name;

    private String imageUrl;

    private String targetUrl;

}
