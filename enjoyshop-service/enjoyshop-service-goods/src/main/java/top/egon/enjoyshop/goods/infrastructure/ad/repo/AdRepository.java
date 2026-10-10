package top.egon.enjoyshop.goods.infrastructure.ad.repo;

import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;
import top.egon.enjoyshop.goods.infrastructure.ad.dao.AdDAO;
import top.egon.enjoyshop.goods.domain.ad.AdDomainQuery;
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
@Repository("adRepository")
@RequiredArgsConstructor
public class AdRepository extends EgonColaRepository<AdDAO, AdPO> {

    @Getter
    @Qualifier("adDAO")
    private final AdDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<AdPO> selectByQuery(@Valid @NotNull AdDomainQuery query,
                                        @Min(1) int limit, @Min(0) long offset) {
        return getBaseMapper().selectByQuery(query, limit, offset);
    }
}
