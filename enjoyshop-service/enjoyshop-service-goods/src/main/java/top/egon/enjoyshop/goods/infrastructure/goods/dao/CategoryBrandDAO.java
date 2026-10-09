package top.egon.enjoyshop.goods.infrastructure.goods.dao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.enjoyshop.goods.infrastructure.goods.po.CategoryBrandPO;

@Mapper
@Validated
public interface CategoryBrandDAO extends EgonColaMapper<CategoryBrandPO> {
    CategoryBrandPO selectActiveByEndpoints(@Param("categoryId") @NotNull @Positive Long categoryId, @Param("brandId") @NotNull @Positive Long brandId,
            @Param("tenantId") @NotNull @Positive Long tenantId);
}
