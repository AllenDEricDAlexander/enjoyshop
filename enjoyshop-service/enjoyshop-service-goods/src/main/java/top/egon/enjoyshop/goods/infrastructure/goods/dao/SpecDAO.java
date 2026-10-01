package top.egon.enjoyshop.goods.infrastructure.goods.dao;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpecPO;

@Mapper
public interface SpecDAO extends EgonColaMapper<SpecPO> {

    List<SpecPO> listByCategoryName(@Param("categoryName") String categoryName);
}
