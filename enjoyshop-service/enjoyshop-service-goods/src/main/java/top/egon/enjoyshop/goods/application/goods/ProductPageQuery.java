package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
public class ProductPageQuery {

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer pageNo = 1;

    @NotNull
    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer pageSize = 10;

    @NotNull
    @Builder.Default
    private Boolean deleted = false;

    @Size(min = 1, max = 64)
    @Pattern(regexp = ".*\\S.*")
    private String sn;

    @Size(min = 1, max = 200)
    @Pattern(regexp = ".*\\S.*")
    private String name;

    @Pattern(regexp = "^(?:0|1)$")
    private String status;

    private Boolean isMarketable;

    @JsonIgnore
    @AssertTrue(message = "recycled products cannot be approved or published")
    public boolean isLifecycleFilterValid() {
        return !Boolean.TRUE.equals(deleted) || (!"1".equals(status) && !Boolean.TRUE.equals(isMarketable));
    }

}
