package top.egon.enjoyshop.goods.infrastructure.goods.dao;

import jakarta.validation.Valid;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.CatalogReferenceBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpecPO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpuPO;

@Mapper
@Validated
public interface SpuDAO extends EgonColaMapper<SpuPO> {
    @Validated(ProductValidationGroup.Detail.class)
    SpuPO selectByLifecycle(@Param("query") @Valid @NotNull ProductDomainQuery query, @Param("tenantId") @NotNull @Positive Long tenantId);
    @Validated(ProductValidationGroup.Detail.class)
    SpuPO lockByLifecycle(@Param("query") @Valid @NotNull ProductDomainQuery query, @Param("tenantId") @NotNull @Positive Long tenantId);
    @Validated(ProductValidationGroup.Page.class)
    List<SpuPO> selectPage(@Param("query") @Valid @NotNull ProductDomainQuery query, @Param("tenantId") @NotNull @Positive Long tenantId,
            @Param("escapedName") String escapedName, @Param("limit") @Min(1) int limit, @Param("offset") @Min(0) long offset);
    @Validated(ProductValidationGroup.Page.class)
    long countPage(@Param("query") @Valid @NotNull ProductDomainQuery query, @Param("tenantId") @NotNull @Positive Long tenantId, @Param("escapedName") String escapedName);
    List<Long> lockCategories(@Param("ids") List<Long> ids, @Param("tenantId") @NotNull @Positive Long tenantId);
    Long lockBrand(@Param("id") @NotNull @Positive Long id, @Param("tenantId") @NotNull @Positive Long tenantId);
    Long lockTemplate(@Param("id") @NotNull @Positive Long id, @Param("tenantId") @NotNull @Positive Long tenantId);
    CatalogReferenceBO selectCatalogReferences(@Param("value") @Valid SpuBO value, @Param("tenantId") @NotNull @Positive Long tenantId);
    List<SpecPO> selectSpecDefinitions(@Param("id") @NotNull @Positive Long templateId, @Param("tenantId") @NotNull @Positive Long tenantId);
    List<ParaDefinitionBO> selectParaDefinitions(@Param("id") @NotNull @Positive Long templateId, @Param("tenantId") @NotNull @Positive Long tenantId);
    int purgeDeletedById(@Param("id") @NotNull @Positive Long id, @Param("version") @NotNull @Min(0) Long version,
            @Param("deletedAt") @NotNull java.time.LocalDateTime deletedAt, @Param("tenantId") @NotNull @Positive Long tenantId);

    /** Immutable SQL-definition value, not a lifecycle entity or a raw Map projection. */
    record ParaDefinitionBO(String name, String options) { }
}
