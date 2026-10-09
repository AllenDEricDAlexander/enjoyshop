package top.egon.enjoyshop.goods.domain.goods;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.egon.enjoyshop.goods.common.validation.ProductCompositionValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import top.egon.enjoyshop.goods.common.enums.ProductSkuDeletionKindEnum;
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
public class SkuBO implements ProductCompositionValidator.ProductSkuInput {

    @Positive
    @Null(groups = ProductValidationGroup.Create.class)
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

    @NotNull
    @Min(0)
    private Integer num;

    @NotNull
    @Min(0)
    private Integer alertNum;

    @NotNull
    @Min(1)
    private Integer price;

    @NotNull
    @Min(0)
    private Integer weight;

    @NotNull
    @Size(max = 20)
    @Builder.Default
    private Map<@NotBlank @Size(max = 120) String, @NotBlank @Size(max = 120) String> spec = Map.of();

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
    @Pattern(regexp = "^(?:1|2)$")
    @Builder.Default
    private String status = "1";

    private Long spuId;

    private String name;

    private Long categoryId;

    private String categoryName;

    private String brandName;

    @Min(0)
    private Integer saleNum;

    @Min(0)
    private Integer commentNum;

    private ProductSkuDeletionKindEnum deletionKind;
    @Override
    @JsonIgnore
    public Long compositionId() {
        return id;
    }

    @Override
    @JsonIgnore
    public String compositionBarcode() {
        return sn;
    }

    @Override
    @JsonIgnore
    public Map<String, String> compositionSpecification() {
        return spec;
    }
}
