package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.enjoyshop.goods.infrastructure.goods.po.CategoryBrandPO;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.CategoryBrandDAO;
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
@Repository("categoryBrandRepository")
@RequiredArgsConstructor
public class CategoryBrandRepository extends EgonColaRepository<CategoryBrandDAO, CategoryBrandPO> {

    @Getter
    @Qualifier("categoryBrandDAO")
    private final CategoryBrandDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public CategoryBrandPO selectActiveByEndpoints(@NotNull @Positive Long categoryId, @NotNull @Positive Long brandId) {
        return baseMapper.selectActiveByEndpoints(categoryId, brandId, EgonColaTenantIdProvider.currentTenantId());
    }
}
