package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AdResult {

    private String id;

    private String name;

    private String position;

    private String imageUrl;

    private String targetUrl;

    private AdStatusEnum status;

    private Instant startAt;

    private Instant endAt;

    private Integer sortOrder;

    private Long version;
}
