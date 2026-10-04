package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.enjoyshop.goods.common.validation.GoodsCatalogQueryValidationGroup;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainQuery;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.SpecDAO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpecPO;

@Slf4j
@Validated
@Repository("specRepository")
@RequiredArgsConstructor
public class SpecRepository extends EgonColaRepository<SpecDAO, SpecPO> {

    @Getter
    @Qualifier("specDAO")
    private final SpecDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<SpecPO> listByCategoryName(
            @Valid @NotNull @ConvertGroup(from = Default.class, to = GoodsCatalogQueryValidationGroup.class)
                    SpecDomainQuery query) {
        return getBaseMapper().listByCategoryName(query.getCategoryName());
    }
}
