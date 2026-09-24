package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Validated
public interface BrandManage {

    BrandResult create(@Valid CreateBrandCommand command);

    BrandResult update(@Valid UpdateBrandCommand command);

    void delete(@Valid DeleteBrandCommand command);

    BrandResult detail(@Valid BrandDetailQuery query);

    PageSlice<BrandResult> page(@Valid BrandPageQuery query);
}
