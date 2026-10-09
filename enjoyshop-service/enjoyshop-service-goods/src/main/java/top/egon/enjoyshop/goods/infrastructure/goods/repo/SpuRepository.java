package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import jakarta.validation.Valid;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import java.util.Map;
import java.util.LinkedHashMap;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.CatalogReferenceBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.service.impl.SpecPersistenceConverter;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpuPO;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.SpuDAO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;

@Slf4j
@Validated
@Repository("spuRepository")
@RequiredArgsConstructor
public class SpuRepository extends EgonColaRepository<SpuDAO, SpuPO> {

    @Getter
    @Qualifier("spuDAO")
    private final SpuDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Validated(ProductValidationGroup.Detail.class)
    public SpuPO selectByLifecycle(@Valid @NotNull ProductDomainQuery query) {
        return baseMapper.selectByLifecycle(query, EgonColaTenantIdProvider.currentTenantId());
    }

    @Validated(ProductValidationGroup.Detail.class)
    public SpuPO lockByLifecycle(@Valid @NotNull ProductDomainQuery query) {
        return baseMapper.lockByLifecycle(query, EgonColaTenantIdProvider.currentTenantId());
    }

    @Validated(ProductValidationGroup.Page.class)
    public List<SpuPO> selectPage(@Valid @NotNull ProductDomainQuery query) {
        return baseMapper.selectPage(query, EgonColaTenantIdProvider.currentTenantId(), escapedName(query.getName()),
                query.getPage().pageSize(), query.getPage().offset());
    }

    @Validated(ProductValidationGroup.Page.class)
    public long countPage(@Valid @NotNull ProductDomainQuery query) {
        return baseMapper.countPage(query, EgonColaTenantIdProvider.currentTenantId(), escapedName(query.getName()));
    }

    @Qualifier("specPersistenceConverterImpl")
    private final SpecPersistenceConverter specConverter;

    public CatalogReferenceBO loadCatalogReferences(@Valid @NotNull SpuBO value) {
        Long tenant = EgonColaTenantIdProvider.currentTenantId();
        List<Long> categories = List.of(value.getCategory1Id(), value.getCategory2Id(), value.getCategory3Id());
        if (baseMapper.lockCategories(categories, tenant).size() != 3
                || baseMapper.lockBrand(value.getBrandId(), tenant) == null
                || baseMapper.lockTemplate(value.getTemplateId(), tenant) == null) {
            return null;
        }
        CatalogReferenceBO result = baseMapper.selectCatalogReferences(value, tenant);
        if (result == null) {
            return null;
        }
        result.setOrderedSpecs(specConverter.toTargetList(baseMapper.selectSpecDefinitions(value.getTemplateId(), tenant)));
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        for (SpuDAO.ParaDefinitionBO definition : baseMapper.selectParaDefinitions(value.getTemplateId(), tenant)) {
            if (parameters.putIfAbsent(definition.name(), SpecPersistenceConverter.splitOptions(definition.options())) != null) {
                return null;
            }
        }
        result.setParaOptions(parameters);
        return result;
    }

    public int purgeDeletedById(@NotNull @Positive Long id, @NotNull @jakarta.validation.constraints.Min(0) Long version,
            @NotNull java.time.LocalDateTime deletedAt) {
        return baseMapper.purgeDeletedById(id, version, deletedAt, EgonColaTenantIdProvider.currentTenantId());
    }

    private static String escapedName(String name) {
        return name == null ? null : "%" + name.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
