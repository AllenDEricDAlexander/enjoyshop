package top.egon.enjoyshop.goods.application.goods;

import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainService;
import top.egon.enjoyshop.goods.application.goods.converter.CreateBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.UpdateBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.DeleteBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.BrandResultConverter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Service("brandManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class BrandManageImpl implements BrandManage {

    @Qualifier("brandDomainService")
    private final BrandDomainService domainService;

    @Qualifier("createBrandCommandConverterImpl")
    private final CreateBrandCommandConverter createConverter;

    @Qualifier("updateBrandCommandConverterImpl")
    private final UpdateBrandCommandConverter updateConverter;

    @Qualifier("deleteBrandCommandConverterImpl")
    private final DeleteBrandCommandConverter deleteConverter;

    @Qualifier("brandResultConverterImpl")
    private final BrandResultConverter resultConverter;

    @Override
    @Transactional
    public BrandResult create(@Valid CreateBrandCommand command) {
        log.info("create {}", "brand");
        return resultConverter.toTarget(domainService.create(createConverter.toTarget(command)));
    }

    @Override
    @Transactional
    public BrandResult update(@Valid UpdateBrandCommand command) {
        BrandBO domain = updateConverter.toTarget(command);
        log.info("update {} id={} version={}", "brand", command.getId(), domain.getVersion());
        return resultConverter.toTarget(domainService.update(domain));
    }

    @Override
    @Transactional
    public void delete(@Valid DeleteBrandCommand command) {
        BrandBO domain = deleteConverter.toTarget(command);
        log.info("delete {} id={} version={}", "brand", command.getId(), domain.getVersion());
        domainService.delete(domain.getId(), domain.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResult detail(@Valid BrandDetailQuery query) {
        return resultConverter.toTarget(domainService.detail(query.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageSlice<BrandResult> page(@Valid BrandPageQuery query) {
        BrandDomainQuery domainQuery = new BrandDomainQuery();
        domainQuery.setPage(query.getPage());
        domainQuery.setName(query.getName());
        domainQuery.setLetter(query.getLetter());
        PageSlice<BrandBO> slice = domainService.query(domainQuery);
        return PageSlice.of(slice.records().stream().map(resultConverter::toTarget).toList(), slice.hasNext());
    }
}
