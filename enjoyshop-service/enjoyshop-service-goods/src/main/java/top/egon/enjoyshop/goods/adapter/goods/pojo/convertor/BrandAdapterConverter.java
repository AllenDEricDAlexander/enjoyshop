package top.egon.enjoyshop.goods.adapter.goods.pojo.convertor;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.BrandListRequest;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.CreateBrandRequest;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.UpdateBrandRequest;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.BrandVO;
import top.egon.enjoyshop.goods.application.goods.BrandPageQuery;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import top.egon.enjoyshop.goods.application.goods.CreateBrandCommand;
import top.egon.enjoyshop.goods.application.goods.UpdateBrandCommand;

import java.util.List;
import java.util.Locale;

/**
 * Adapter-layer normalisation and projection boundary (Spec §10.4 mapping ownership).
 *
 * <p>{@code BrandResult -> BrandVO} is the declared primary direction of the Egon
 * {@link BaseForwardConverter}; the request-to-command and request-to-query mappings are additional
 * one-way projections on the same interface, because a single Java interface cannot inherit two
 * parameterisations of the same generic contract ({@code PLAN-CLAR-004}).</p>
 *
 * <p>{@code unmappedTargetPolicy = ERROR} is the drift guard: a new column on a target type fails
 * the build until the mapping says what to do with it, which is what keeps {@code BrandVO} from
 * silently growing a leaked scoping field.</p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BrandAdapterConverter extends BaseForwardConverter<BrandResult, BrandVO> {

    /**
     * {@code API-003}: the nullable {@code seq} becomes the server default {@code 0} here so the
     * command can keep {@code @NotNull}.
     */
    @Mapping(target = "name", source = "name", qualifiedByName = "normalizeName")
    @Mapping(target = "letter", source = "letter", qualifiedByName = "normalizeLetter")
    @Mapping(target = "seq", expression = "java(source.seq() == null ? 0 : source.seq())")
    CreateBrandCommand toTarget(CreateBrandRequest source);

    /**
     * {@code API-004}: the path identifier and the caller's revision become the guarded write
     * condition; {@code image} is mapped verbatim so an explicit null clears the column.
     */
    @Mapping(target = "id", source = "brandId")
    @Mapping(target = "expectedVersion", source = "request.version")
    @Mapping(target = "name", source = "request.name", qualifiedByName = "normalizeName")
    @Mapping(target = "letter", source = "request.letter", qualifiedByName = "normalizeLetter")
    UpdateBrandCommand toCommand(Long brandId, UpdateBrandRequest request);

    /**
     * {@code API-001}: blank filters are treated as absent; {@code seqMin}/{@code seqMax} are not
     * DDL columns and therefore stay separate statement parameters ({@code PLAN-CLAR-004}).
     */
    @Mapping(target = "name", source = "request.name", qualifiedByName = "nullIfBlank")
    @Mapping(target = "letter", source = "request.letter", qualifiedByName = "nullIfBlankUpper")
    @Mapping(target = "page", source = "page")
    BrandPageQuery toPageQuery(BrandListRequest request, PageQuery page);

    @Override
    @Mapping(target = "id", expression = "java(source.getId() == null ? null : Long.valueOf(source.getId()))")
    BrandVO toTarget(BrandResult source);

    List<BrandVO> toVOList(List<BrandResult> results);

    @Named("normalizeName")
    static String normalizeName(String raw) {
        return raw == null ? null : StringUtils.normalizeSpace(raw.trim());
    }

    @Named("normalizeLetter")
    static String normalizeLetter(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    @Named("nullIfBlank")
    static String nullIfBlank(String raw) {
        return raw == null || raw.isBlank() ? null : StringUtils.normalizeSpace(raw.trim());
    }

    @Named("nullIfBlankUpper")
    static String nullIfBlankUpper(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim().toUpperCase(Locale.ROOT);
    }
}
