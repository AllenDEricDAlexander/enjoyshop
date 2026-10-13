package top.egon.enjoyshop.goods.application.goods;

import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface SpecManage {

    List<SpecResult> listSpecsByCategoryName(SpecListQuery query);
}
