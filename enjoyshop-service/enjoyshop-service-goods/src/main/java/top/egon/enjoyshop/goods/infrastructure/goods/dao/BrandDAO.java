package top.egon.enjoyshop.goods.infrastructure.goods.dao;

import top.egon.enjoyshop.goods.infrastructure.goods.po.BrandPO;
import org.apache.ibatis.annotations.Mapper;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface BrandDAO extends EgonColaMapper<BrandPO> {
    List<BrandPO> selectByQuery(@Param("query") BrandDomainQuery query, @Param("limit") int limit, @Param("offset") long offset);
}
