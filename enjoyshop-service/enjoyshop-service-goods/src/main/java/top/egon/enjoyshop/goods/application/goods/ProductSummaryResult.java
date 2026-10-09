package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class ProductSummaryResult {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long id;

    private String sn;

    private String name;

    private String image;

    private String status;

    private Boolean isMarketable;

    private Boolean isDelete;

    private Long version;
}
