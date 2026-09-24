package top.egon.enjoyshop.goods.adapter.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.convertor.BrandAdapterConverter;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.BrandListRequest;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.CreateBrandRequest;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.UpdateBrandRequest;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.BrandVO;
import top.egon.enjoyshop.goods.application.goods.BrandDetailQuery;
import top.egon.enjoyshop.goods.application.goods.BrandManage;
import top.egon.enjoyshop.goods.application.goods.BrandPageQuery;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import top.egon.enjoyshop.goods.application.goods.DeleteBrandCommand;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

import java.net.URI;

/**
 * The five {@code API-001}-{@code API-005} contracts of Spec §9.2 (REQ-007..REQ-014, REQ-019).
 *
 * <p>Every response is an envelope: {@link ResultRecord} for a single resource,
 * {@link PageResultRecord} for the catalog, and {@code data=true} for the soft delete, because
 * {@code DEC-109} makes the envelope shape invariant. The controller is the only place that decides
 * between full-catalog and paged mode, and the only place that can judge the two-sided
 * {@code seqMin}/{@code seqMax} range, so both live here rather than in a validator.</p>
 *
 * <p>There is no identifier parsing helper any more: {@code brandId} is a {@link Long} path
 * variable guarded by {@link Positive}, so a non-numeric value fails binding as a type mismatch and
 * a non-positive one fails as a constraint violation, both of which {@code BaseExceptionHandler}
 * maps to 400 / 610101.</p>
 */
@RestController("brandController")
@RequestMapping("/api/v1/brands")
@Validated
@Tag(name = "brand", description = "商品品牌字典")
@RequiredArgsConstructor
@Slf4j
public class BrandController {

    private static final String BASE_PATH = "/api/v1/brands";

    @Qualifier("brandManage")
    private final BrandManage manage;

    @Qualifier("brandAdapterConverterImpl")
    private final BrandAdapterConverter converter;

    /**
     * {@code API-001}: absence of both page parameters selects full-catalog mode with the
     * {@link PageQuery#MAX_PAGE_SIZE} ceiling; supplying either one selects paged mode.
     */
    @GetMapping
    @Operation(operationId = "listBrands", summary = "查询品牌集合")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "参数不合法（含 seq 区间倒置）"),
            @ApiResponse(responseCode = "500", description = "服务内部错误")
    })
    public PageResultRecord<BrandVO> listBrands(@Valid @ParameterObject BrandListRequest request) {
        if (request.seqMin() != null && request.seqMax() != null && request.seqMax() < request.seqMin()) {
            throw new CommonException(GoodsErrorStatus.BRAND_REQUEST_INVALID);
        }
        PageQuery page = toPageQuery(request);
        PageResultRecord<BrandResult> payload =
                manage.listBrands(converter.toPageQuery(request, page), request.seqMin(), request.seqMax());
        return PageResultRecord.success(converter.toVOList(payload.records()),
                payload.page().total(), payload.page().pageNo(), payload.page().pageSize());
    }

    @GetMapping("/{brandId}")
    @Operation(operationId = "getBrand", summary = "查询品牌详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "品牌标识不合法"),
            @ApiResponse(responseCode = "404", description = "品牌不存在或已下架"),
            @ApiResponse(responseCode = "500", description = "服务内部错误")
    })
    public ResultRecord<BrandVO> getBrand(@Positive @PathVariable Long brandId) {
        return ResultRecord.success(converter.toTarget(manage.detail(new BrandDetailQuery().setId(brandId))));
    }

    /**
     * {@code API-003}: 201 plus {@code Location} is the only created-contract in this slice, and the
     * location is derived from the identifier the server assigned, never from the request.
     */
    @PostMapping
    @Operation(operationId = "createBrand", summary = "新增品牌")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功", headers =
            @Header(name = "Location", description = "新建品牌资源地址", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "请求体校验失败"),
            @ApiResponse(responseCode = "401", description = "缺少操作者上下文"),
            @ApiResponse(responseCode = "409", description = "品牌名称已存在"),
            @ApiResponse(responseCode = "500", description = "服务内部错误")
    })
    public ResponseEntity<ResultRecord<BrandVO>> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        BrandVO created = converter.toTarget(manage.create(converter.toTarget(request)));
        log.info("http create brand id={}", created.id());
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id()))
                .body(ResultRecord.success(created));
    }

    @PutMapping("/{brandId}")
    @Operation(operationId = "updateBrand", summary = "更新品牌")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功，data.version 为新版本"),
            @ApiResponse(responseCode = "400", description = "请求体校验失败"),
            @ApiResponse(responseCode = "401", description = "缺少操作者上下文"),
            @ApiResponse(responseCode = "404", description = "品牌不存在或已下架"),
            @ApiResponse(responseCode = "409", description = "版本冲突或名称已被占用"),
            @ApiResponse(responseCode = "500", description = "服务内部错误")
    })
    public ResultRecord<BrandVO> updateBrand(@Positive @PathVariable Long brandId,
            @Valid @RequestBody UpdateBrandRequest request) {
        log.info("http update brand id={}", brandId);
        return ResultRecord.success(converter.toTarget(manage.update(converter.toCommand(brandId, request))));
    }

    /**
     * {@code API-005}: the envelope invariant of {@code DEC-109} means a successful delete answers
     * 200 with {@code data=true} rather than 204.
     */
    @DeleteMapping("/{brandId}")
    @Operation(operationId = "deleteBrand", summary = "下架品牌")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "下架成功"),
            @ApiResponse(responseCode = "400", description = "version 缺失或不合法"),
            @ApiResponse(responseCode = "401", description = "缺少操作者上下文"),
            @ApiResponse(responseCode = "404", description = "品牌不存在或已下架"),
            @ApiResponse(responseCode = "409", description = "版本冲突"),
            @ApiResponse(responseCode = "500", description = "服务内部错误")
    })
    public ResultRecord<Boolean> deleteBrand(@Positive @PathVariable Long brandId,
            @NotNull @PositiveOrZero @RequestParam Long version) {
        log.info("http delete brand id={} version={}", brandId, version);
        manage.delete(new DeleteBrandCommand().setId(brandId).setExpectedVersion(version));
        return ResultRecord.success(Boolean.TRUE);
    }

    private static PageQuery toPageQuery(BrandListRequest request) {
        if (request.pageNo() == null && request.pageSize() == null) {
            return new PageQuery(PageQuery.DEFAULT_PAGE_NO, PageQuery.MAX_PAGE_SIZE);
        }
        return new PageQuery(request.pageNo() == null ? PageQuery.DEFAULT_PAGE_NO : request.pageNo(),
                request.pageSize() == null ? PageQuery.DEFAULT_PAGE_SIZE : request.pageSize());
    }
}
