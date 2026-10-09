package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
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
public class SpuResult {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long id;

    private String sn;

    private String name;

    private String caption;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long brandId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long category1Id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long category2Id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long category3Id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long templateId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(type = "string", pattern = "[0-9]+")
    private Long freightId;

    private String image;

    private List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$") String> images;

    private List<@NotBlank @Size(max = 100) String> saleService;

    private String introduction;

    private Boolean isEnableSpec;

    private Map<@NotBlank @Size(max = 120) String, @NotNull @Size(min = 1, max = 100) List<@NotBlank @Size(max = 120) String>> specItems;

    private Map<@NotBlank @Size(max = 120) String, @NotBlank @Size(max = 1000) String> paraItems;

    private Integer saleNum;

    private Integer commentNum;

    private String status;

    private Boolean isMarketable;

    private Boolean isDelete;

    private Long version;

    private Instant createTime;

    private Instant updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    private LocalDateTime deletedAt;
}
