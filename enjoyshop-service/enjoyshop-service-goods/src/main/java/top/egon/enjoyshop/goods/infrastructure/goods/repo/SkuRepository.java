package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SkuPO;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.SkuDAO;
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
@Repository("skuRepository")
@RequiredArgsConstructor
public class SkuRepository extends EgonColaRepository<SkuDAO, SkuPO> {

    @Getter
    @Qualifier("skuDAO")
    private final SkuDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<SkuPO> selectBySpuLifecycle(@NotNull @Positive Long spuId, boolean deleted) {
        return baseMapper.selectBySpuLifecycle(spuId, deleted, EgonColaTenantIdProvider.currentTenantId());
    }

    public List<SkuPO> selectAllBySpuForPurge(@NotNull @Positive Long spuId) {
        return baseMapper.selectAllBySpuForPurge(spuId, EgonColaTenantIdProvider.currentTenantId());
    }

    public int purgeAllBySpu(@NotNull @Positive Long spuId, @NotNull @jakarta.validation.constraints.Min(0) Long version,
            @NotNull java.time.LocalDateTime deletedAt) {
        return baseMapper.purgeAllBySpu(spuId, version, deletedAt, EgonColaTenantIdProvider.currentTenantId());
    }
}
