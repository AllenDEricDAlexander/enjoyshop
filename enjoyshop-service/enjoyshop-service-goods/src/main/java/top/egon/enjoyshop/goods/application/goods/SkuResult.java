package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SkuResult {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long spuId;

    private String sn;

    private Integer num;

    private Integer alertNum;

    private Integer price;

    private Integer weight;

    private Map<@NotBlank @Size(max = 120) String, @NotBlank @Size(max = 120) String> spec;

    private String image;

    private List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$") String> images;

    private String status;

    private String name;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long categoryId;

    private String categoryName;

    private String brandName;

    private Integer saleNum;

    private Integer commentNum;

    private Instant createTime;

    private Instant updateTime;
}
