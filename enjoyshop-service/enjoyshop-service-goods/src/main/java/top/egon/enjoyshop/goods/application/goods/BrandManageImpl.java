package top.egon.enjoyshop.goods.application.goods;

import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainService;
import top.egon.enjoyshop.goods.application.goods.converter.CreateBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.UpdateBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.DeleteBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.BrandResultConverter;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Service("brandManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class BrandManageImpl implements BrandManage {

    /** MDC key carrying the authenticated operator, published by the request filter. */
    private static final String OPERATOR_MDC_KEY = "userId";

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
        requireOperator();
        requireNameAvailable(command.getName(), null);
        log.info("create {}", "brand");
        return resultConverter.toTarget(domainService.create(createConverter.toTarget(command)));
    }

    @Override
    @Transactional
    public BrandResult update(@Valid UpdateBrandCommand command) {
        requireOperator();
        requireNameAvailable(command.getName(), command.getId());
        BrandBO domain = updateConverter.toTarget(command);
        log.info("update {} id={} version={}", "brand", command.getId(), domain.getVersion());
        return resultConverter.toTarget(domainService.update(domain));
    }

    @Override
    @Transactional
    public void delete(@Valid DeleteBrandCommand command) {
        requireOperator();
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

    @Override
    @Transactional(readOnly = true)
    public PageResultRecord<BrandResult> listBrands(@Valid BrandPageQuery query, Integer seqMin, Integer seqMax) {
        BrandDomainQuery domainQuery = BrandDomainQuery.builder()
                .name(query.getName())
                .letter(query.getLetter())
                .page(query.getPage())
                .build();
        List<BrandResult> records = resultConverter.toTargetList(domainService.listPage(domainQuery, seqMin, seqMax));
        long total = domainService.countPage(domainQuery, seqMin, seqMax);
        return PageResultRecord.success(records, total, query.getPage().pageNo(), query.getPage().pageSize());
    }

    /**
     * Every brand write needs an identifiable operator; {@code userId} is published to MDC by the
     * request filter, so a blank value means the write must be refused before touching the domain.
     */
    private void requireOperator() {
        String operator = MDC.get(OPERATOR_MDC_KEY);
        if (operator == null || operator.isBlank()) {
            throw new CommonException(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED);
        }
    }

    /**
     * Pre-check against the tenant partial unique index so a plain "name taken" answers 409 610103
     * instead of surfacing a duplicate key failure. {@code excludeId} is the edited row, so keeping
     * the current name on update stays valid.
     */
    private void requireNameAvailable(String name, Long excludeId) {
        if (domainService.existsActiveByName(name, excludeId)) {
            throw new CommonException(GoodsErrorStatus.BRAND_NAME_DUPLICATED);
        }
    }
}
