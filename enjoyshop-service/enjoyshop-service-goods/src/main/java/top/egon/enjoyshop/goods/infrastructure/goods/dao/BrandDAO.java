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

    /**
     * Catalog ordering (sequence descending, id as the tie breaker) with the optional name prefix,
     * letter and closed seq range predicates; {@code name} is already escaped by the Repository facade.
     */
    List<BrandPO> listBrandPage(@Param("name") String namePrefixEscaped, @Param("letter") String letter,
            @Param("seqMin") Integer seqMin, @Param("seqMax") Integer seqMax,
            @Param("limit") int limit, @Param("offset") long offset);

    /** Total row count for {@link #listBrandPage} under exactly the same predicates. */
    long countBrandPage(@Param("name") String namePrefixEscaped, @Param("letter") String letter,
            @Param("seqMin") Integer seqMin, @Param("seqMax") Integer seqMax);

    /** Active rows carrying the exact name; {@code excludeId} drops the row being updated. */
    long countActiveByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /** Narrower overload of the shared active read so the facade keeps a {@link Long} contract. */
    BrandPO selectActiveById(@Param("id") Long id);
}
