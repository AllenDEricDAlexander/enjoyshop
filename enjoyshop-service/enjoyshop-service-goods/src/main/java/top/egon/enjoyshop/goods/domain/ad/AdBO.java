package top.egon.enjoyshop.goods.domain.ad;

import java.time.Instant;
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
public class AdBO {

    private Long id;

    private Long version;

    private String name;

    private String position;

    private String imageUrl;

    private String targetUrl;

    private String status;

    private Instant startAt;

    private Instant endAt;

    private Integer sortOrder;
}
