package top.egon.enjoyshop.goods.infrastructure.ad.dao;

import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;

@Mapper
@Validated
public interface AdDAO extends EgonColaMapper<AdPO> {
    @Validated(EgonColaModelValidationGroups.Query.class)
    List<AdPO> selectByQuery(@Param("query") @Valid @NotNull(groups = EgonColaModelValidationGroups.Query.class) AdDomainQuery query,
                                  @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit,
                                  @Param("offset") @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class) long offset);
}
