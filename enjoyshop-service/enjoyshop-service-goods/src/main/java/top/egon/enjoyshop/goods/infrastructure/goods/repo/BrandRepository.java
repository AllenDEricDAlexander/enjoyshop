package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import top.egon.enjoyshop.goods.infrastructure.goods.po.BrandPO;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.BrandDAO;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
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
import top.egon.enjoyshop.goods.common.validation.GoodsCatalogQueryValidationGroup;

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

    /** Active single row read; a null result means "not visible" and the caller answers 404. */
    public BrandPO selectActiveById(@NotNull @Positive Long id) {
        return getBaseMapper().selectActiveById(id);
    }

    /**
     * Active rows using the exact name. {@code excludeId} stays nullable on purpose: the create
     * duplicate check passes null, the update check passes the row being edited.
     */
    public long countActiveByName(@NotBlank @Size(max = 120) String name, @Positive Long excludeId) {
        return getBaseMapper().countActiveByName(name, excludeId);
    }

    /** Catalog ordering; paging metadata is read from the normalized {@link BrandDomainQuery}. */
    public List<BrandPO> selectBrandPage(
                                        @Valid @NotNull @ConvertGroup(from = Default.class, to = GoodsCatalogQueryValidationGroup.class)
                                        BrandDomainQuery query,
                                        @Min(0) Integer seqMin, @Min(0) Integer seqMax) {
        return getBaseMapper().listBrandPage(likePrefix(query.getName()), query.getCategoryName(), query.getLetter(),
                seqMin, seqMax, query.getPage().pageSize(), query.getPage().offset());
    }

    /** Total rows for {@link #selectBrandPage} under exactly the same predicates. */
    public long countBrandPage(
                               @Valid @NotNull @ConvertGroup(from = Default.class, to = GoodsCatalogQueryValidationGroup.class)
                               BrandDomainQuery query,
                               @Min(0) Integer seqMin, @Min(0) Integer seqMax) {
        return getBaseMapper().countBrandPage(likePrefix(query.getName()), query.getCategoryName(),
                query.getLetter(), seqMin, seqMax);
    }

    /**
     * Turns a raw name into a prefix pattern for {@code LIKE ... ESCAPE '\'}: backslash first so the
     * escapes added for {@code %} and {@code _} are not doubled, blank input drops the predicate.
     */
    static String likePrefix(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
