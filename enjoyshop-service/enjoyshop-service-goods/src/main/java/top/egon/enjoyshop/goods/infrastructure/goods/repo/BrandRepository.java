package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import top.egon.enjoyshop.goods.infrastructure.goods.po.BrandPO;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.BrandDAO;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

@Slf4j
@Validated
@Repository("brandRepository")
@RequiredArgsConstructor
public class BrandRepository extends EgonColaRepository<BrandDAO, BrandPO> {

    @Getter
    @Qualifier("brandDAO")
    private final BrandDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<BrandPO> selectByQuery(@Valid @NotNull BrandDomainQuery query,
                                        @Min(1) int limit, @Min(0) long offset) {
        return getBaseMapper().selectByQuery(query, limit, offset);
    }
}
