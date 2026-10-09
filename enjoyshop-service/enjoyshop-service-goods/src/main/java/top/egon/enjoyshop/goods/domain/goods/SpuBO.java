package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class SpuBO {

    @Positive
    @Null(groups = ProductValidationGroup.Create.class)
    @NotNull(groups = ProductValidationGroup.Update.class)
    private Long id;

    @Min(0)
    private Long version;

    private Long tenantId;

    private String createUserId;

    private Instant createTime;

    private String updateUserId;

    private Instant updateTime;

    private LocalDateTime deletedAt;

    @NotBlank
    @Size(max = 64)
    private String sn;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    @Size(max = 300)
    @Builder.Default
    private String caption = "";

    @NotNull
    @Positive
    private Long brandId;

    @NotNull
    @Positive
    private Long category1Id;

    @NotNull
    @Positive
    private Long category2Id;

    @NotNull
    @Positive
    private Long category3Id;

    @NotNull
    @Positive
    private Long templateId;

    @NotNull
    @PositiveOrZero
    @Builder.Default
    private Long freightId = 0L;

    @NotNull
    @Size(max = 500)
    @Pattern(regexp = "^(?:|https://\\S+)$")
    @Builder.Default
    private String image = "";

    @NotNull
    @Size(max = 10)
    @Builder.Default
    private List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$") String> images = List.of();

    @NotNull
    @Size(max = 20)
    @Builder.Default
    private List<@NotBlank @Size(max = 100) String> saleService = List.of();

    @NotNull
    @Size(max = 20000)
    @Builder.Default
    private String introduction = "";

    @NotNull
    private Boolean isEnableSpec;

    @NotNull
    @Size(max = 20)
    @Builder.Default
    private Map<@NotBlank @Size(max = 120) String, @NotNull @Size(min = 1, max = 100) List<@NotBlank @Size(max = 120) String>> specItems = Map.of();

    @NotNull
    @Size(max = 100)
    @Builder.Default
    private Map<@NotBlank @Size(max = 120) String, @NotBlank @Size(max = 1000) String> paraItems = Map.of();

    @Min(0)
    private Integer saleNum;

    @Min(0)
    private Integer commentNum;

    @Pattern(regexp = "^(?:0|1)$")
    private String status;

    private Boolean isMarketable;
}
