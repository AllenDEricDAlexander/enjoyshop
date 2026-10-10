package top.egon.enjoyshop.goods.infrastructure.ad.repo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import top.egon.enjoyshop.goods.infrastructure.ad.dao.AdDAO;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;

@Slf4j
@Validated
@Repository("adRepository")
@RequiredArgsConstructor
public class AdRepository extends EgonColaRepository<AdDAO, AdPO> {

    @Getter
    @Qualifier("adDAO")
    private final AdDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Validated(AdValidationGroup.Detail.class)
    public AdPO selectByLifecycle(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.selectByLifecycle(query, requireTenant(query));
    }

    @Validated(AdValidationGroup.Detail.class)
    public AdPO lockByLifecycle(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.lockByLifecycle(query, requireTenant(query));
    }

    @Validated(AdValidationGroup.Page.class)
    public List<AdPO> selectPage(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.selectPage(query, requireTenant(query), query.getPage().pageSize(), query.getPage().offset());
    }

    @Validated(AdValidationGroup.Page.class)
    public long countPage(@Valid @NotNull AdDomainQuery query) {
        return baseMapper.countPage(query, requireTenant(query));
    }

    @Validated(AdValidationGroup.PublicRead.class)
    public List<AdPO> selectCandidates(@Valid @NotNull AdDomainQuery query, @Min(1) int limit) {
        return baseMapper.selectCandidates(query, requireTenant(query), limit);
    }

    private static Long requireTenant(AdDomainQuery query) {
        Long currentTenant = EgonColaTenantIdProvider.currentTenantId();
        if (!Objects.equals(currentTenant, query.getTenantId())) {
            throw new CommonException(GoodsErrorStatus.AD_FORBIDDEN);
        }
        return currentTenant;
    }
}
