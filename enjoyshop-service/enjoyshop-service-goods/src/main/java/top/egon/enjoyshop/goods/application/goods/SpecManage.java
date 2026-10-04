package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface SpecManage {

    List<SpecResult> listSpecsByCategoryName(@Valid @NotNull SpecListQuery query);
}
