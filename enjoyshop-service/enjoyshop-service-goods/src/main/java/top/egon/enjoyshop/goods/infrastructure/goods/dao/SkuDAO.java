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
import top.egon.enjoyshop.goods.infrastructure.goods.po.SkuPO;

@Mapper
@Validated
public interface SkuDAO extends EgonColaMapper<SkuPO> {
    List<SkuPO> selectBySpuLifecycle(@Param("spuId") @NotNull @Positive Long spuId, @Param("deleted") boolean deleted, @Param("tenantId") @NotNull @Positive Long tenantId);
    List<SkuPO> selectAllBySpuForPurge(@Param("spuId") @NotNull @Positive Long spuId, @Param("tenantId") @NotNull @Positive Long tenantId);
    int purgeAllBySpu(@Param("spuId") @NotNull @Positive Long spuId, @Param("version") @NotNull @Min(0) Long version,
            @Param("deletedAt") @NotNull java.time.LocalDateTime deletedAt, @Param("tenantId") @NotNull @Positive Long tenantId);
}
