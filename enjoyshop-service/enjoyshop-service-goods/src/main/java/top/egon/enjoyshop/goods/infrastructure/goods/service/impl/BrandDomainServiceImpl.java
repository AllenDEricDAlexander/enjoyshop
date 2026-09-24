package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainService;
import top.egon.enjoyshop.goods.infrastructure.goods.po.BrandPO;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.BrandRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

import java.util.ArrayList;
import java.util.List;

@Service("brandDomainService")
@Validated
@Slf4j
@RequiredArgsConstructor
public class BrandDomainServiceImpl implements BrandDomainService {

    @Qualifier("brandRepository")
    private final BrandRepository repository;

    @Qualifier("brandPersistenceConverterImpl")
    private final BrandPersistenceConverter converter;

    @Override
    @Transactional
    public BrandBO create(@Valid BrandBO value) {
        BrandPO po = converter.toSource(value);
        if (!repository.save(po)) {
            throw top.egon.enjoyshop.goods.common.error.GoodsErrorMapper.zeroRows("create", po.getId(), po.getVersion());
        }
        log.info("created {} id={}", "brand", po.getId());
        return converter.toTarget(po);
    }

    @Override
    @Transactional
    public BrandBO update(@Valid BrandBO value) {
        BrandPO current = repository.getById(value.getId());
        if (current == null) {
            throw top.egon.enjoyshop.goods.common.error.GoodsErrorMapper.missing(value.getId());
        }
        BrandPO po = converter.toSource(value);
        po.setId(current.getId());
        po.setTenantId(current.getTenantId());
        po.setCreateUserId(current.getCreateUserId());
        po.setCreateTime(current.getCreateTime());
        po.setVersion(value.getVersion());
        if (!repository.updateById(po)) {
            throw top.egon.enjoyshop.goods.common.error.GoodsErrorMapper.zeroRows("update", po.getId(), po.getVersion());
        }
        log.info("updated {} id={} version={}", "brand", po.getId(), po.getVersion());
        return converter.toTarget(po);
    }

    @Override
    @Transactional
    public void delete(Long id, Long expectedVersion) {
        BrandPO po = new BrandPO();
        po.setId(id);
        po.setVersion(expectedVersion);
        if (!repository.removeById(po)) {
            throw top.egon.enjoyshop.goods.common.error.GoodsErrorMapper.zeroRows("delete", id, expectedVersion);
        }
        log.info("deleted {} id={} version={}", "brand", id, expectedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandBO detail(Long id) {
        BrandPO po = repository.getById(id);
        if (po == null) {
            throw top.egon.enjoyshop.goods.common.error.GoodsErrorMapper.missing(id);
        }
        return converter.toTarget(po);
    }

    @Override
    @Transactional(readOnly = true)
    public PageSlice<BrandBO> query(@Valid BrandDomainQuery query) {
        int limit = query.getPage().pageSize();
        List<BrandPO> rows = new ArrayList<>(repository.selectByQuery(query, limit + 1, query.getPage().offset()));
        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }
        return PageSlice.of(rows.stream().map(converter::toTarget).toList(), hasNext);
    }
}
