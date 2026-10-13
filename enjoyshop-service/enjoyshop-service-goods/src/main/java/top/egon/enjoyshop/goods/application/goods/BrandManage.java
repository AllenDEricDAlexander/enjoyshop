package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Validated
public interface BrandManage {

    BrandResult create(CreateBrandCommand command);

    BrandResult update(UpdateBrandCommand command);

    void delete(DeleteBrandCommand command);

    BrandResult detail(BrandDetailQuery query);

    PageSlice<BrandResult> page(BrandPageQuery query);

    /**
     * Paged catalog list carrying the total row count (PLAN-CLAR-010): the envelope stays a plain
     * records + total + page-metadata carrier for the adapter to project.
     */
    PageResultRecord<BrandResult> listBrands(BrandPageQuery query,
                                             @Min(0) Integer seqMin, @Min(0) Integer seqMax);
}
