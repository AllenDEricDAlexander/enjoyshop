package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.goods.domain.goods.SpecBO;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainService;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpecPO;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.SpecRepository;

@Service("specDomainService")
@Validated
@Slf4j
@RequiredArgsConstructor
public class SpecDomainServiceImpl implements SpecDomainService {

    @Qualifier("specRepository")
    private final SpecRepository repository;

    @Qualifier("specPersistenceConverterImpl")
    private final SpecPersistenceConverter converter;

    @Override
    @Transactional(readOnly = true)
    public List<SpecBO> listByCategoryName(SpecDomainQuery query) {
        List<SpecPO> rows = repository.listByCategoryName(query);
        return converter.toTargetList(rows);
    }
}
