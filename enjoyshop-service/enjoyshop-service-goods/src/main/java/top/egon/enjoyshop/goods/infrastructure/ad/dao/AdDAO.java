package top.egon.enjoyshop.goods.infrastructure.ad.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;

@Mapper
@Validated
public interface AdDAO extends EgonColaMapper<AdPO> {

    @Validated(AdValidationGroup.Detail.class)
    AdPO selectByLifecycle(@Param("query") @Valid @NotNull AdDomainQuery query,
            @Param("tenantId") @NotNull @Positive Long tenantId);

    @Validated(AdValidationGroup.Detail.class)
    AdPO lockByLifecycle(@Param("query") @Valid @NotNull AdDomainQuery query,
            @Param("tenantId") @NotNull @Positive Long tenantId);

    @Validated(AdValidationGroup.Page.class)
    List<AdPO> selectPage(@Param("query") @Valid @NotNull AdDomainQuery query,
            @Param("tenantId") @NotNull @Positive Long tenantId,
            @Param("limit") @Min(1) int limit,
            @Param("offset") @Min(0) long offset);

    @Validated(AdValidationGroup.Page.class)
    long countPage(@Param("query") @Valid @NotNull AdDomainQuery query,
            @Param("tenantId") @NotNull @Positive Long tenantId);

    @Validated(AdValidationGroup.PublicRead.class)
    List<AdPO> selectCandidates(@Param("query") @Valid @NotNull AdDomainQuery query,
            @Param("tenantId") @NotNull @Positive Long tenantId,
            @Param("limit") @Min(1) int limit);
}
