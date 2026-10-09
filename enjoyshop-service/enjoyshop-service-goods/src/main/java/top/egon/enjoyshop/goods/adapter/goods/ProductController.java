package top.egon.enjoyshop.goods.adapter.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.application.goods.*;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

@RestController("productController")
@RequestMapping(value = "/api/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "product", description = "商品管理")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "请求字段或格式不合法", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductRequestError"))),
    @ApiResponse(responseCode = "401", description = "缺少有效USER身份", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductFilterError"))),
    @ApiResponse(responseCode = "403", description = "权限被拒绝", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductAuthorizationError"))),
    @ApiResponse(responseCode = "500", description = "服务内部错误", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
    @ApiResponse(responseCode = "503", description = "认证、授权或ID生成不可用", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductAuthorizationError")))
})
@Slf4j
@RequiredArgsConstructor
public class ProductController {
    private static final String BASE_PATH = "/api/v1/products";
    @Qualifier("productManage") private final ProductManage manage;

    @ModelAttribute
    void requestBoundary(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Set<String> allowed = Set.of();
        if (request.getMethod().equals("GET")) {
            allowed = path.equals(BASE_PATH) ? Set.of("pageNo", "pageSize", "deleted", "sn", "name", "status", "isMarketable") : Set.of("deleted");
        } else if (request.getMethod().equals("DELETE")) { allowed = Set.of("version"); }
        for (var entry : request.getParameterMap().entrySet()) {
            if (!allowed.contains(entry.getKey()) || entry.getValue().length != 1) { throw invalid(); }
        }
    }

    @InitBinder
    void strictQueryBooleans(WebDataBinder binder) {
        binder.registerCustomEditor(Boolean.class, new java.beans.PropertyEditorSupport() {
            @Override public void setAsText(String text) {
                if (!"true".equals(text) && !"false".equals(text)) { throw invalid(); }
                setValue(Boolean.valueOf(text));
            }
        });
    }

    @GetMapping
    @Operation(operationId = "listProducts", summary = "查询商品或回收站列表")
    @ApiResponse(responseCode = "200", description = "有界分页列表", useReturnTypeSchema = true)
    public PageResultRecord<ProductSummaryResult> listProducts(@Valid @ParameterObject ProductPageQuery query) {
        return manage.page(query);
    }

    @GetMapping("/{spuId}")
    @Operation(operationId = "getProduct", summary = "查询商品聚合详情")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "聚合详情", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<ProductResult> getProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @RequestParam(defaultValue = "false") Boolean deleted) {
        return ResultRecord.success(manage.detail(ProductIdQuery.builder().spuId(id(spuId)).deleted(deleted).build()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "createProduct", summary = "新增商品聚合")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "新增成功", useReturnTypeSchema = true,
        headers = @Header(name = "Location", description = "新商品资源URI", schema = @Schema(type = "string", format = "uri"))),
        @ApiResponse(responseCode = "409", description = "货号或条码占用", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(ref = "#/components/schemas/ProductCreateInput")))
    public ResponseEntity<ResultRecord<ProductResult>> createProduct(@RequestBody @Validated(ProductValidationGroup.Create.class) ProductSaveCommand command) {
        ProductResult result = manage.create(command);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + result.getSpu().getId())).body(ResultRecord.success(result));
    }

    @PutMapping(value = "/{spuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "updateProduct", summary = "修改下架商品并重置审核")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "修改后的聚合", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本、状态或业务键冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(ref = "#/components/schemas/ProductUpdateInput")))
    public ResultRecord<ProductResult> updateProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @RequestBody @Validated(ProductValidationGroup.Update.class) ProductSaveCommand command) {
        return ResultRecord.success(manage.update(id(spuId), command));
    }

    @PutMapping(value = "/{spuId}/approval", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "approveProduct", summary = "审核通过并上架")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "商品状态", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本、状态或业务键冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<ProductStateResult> approveProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @RequestBody @Validated(ProductValidationGroup.HttpStateInput.class) ProductLifecycleCommand command) {
        command.setSpuId(id(spuId));
        return ResultRecord.success(manage.audit(command));
    }

    @PutMapping(value = "/{spuId}/withdrawal", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "withdrawProduct", summary = "下架商品")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "商品状态", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本、状态或业务键冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<ProductStateResult> withdrawProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @RequestBody @Validated(ProductValidationGroup.HttpStateInput.class) ProductLifecycleCommand command) {
        command.setSpuId(id(spuId));
        return ResultRecord.success(manage.pull(command));
    }

    @PutMapping(value = "/{spuId}/publication", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "publishProduct", summary = "上架已审核商品")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "商品状态", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本、状态或业务键冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<ProductStateResult> publishProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @RequestBody @Validated(ProductValidationGroup.HttpStateInput.class) ProductLifecycleCommand command) {
        command.setSpuId(id(spuId));
        return ResultRecord.success(manage.put(command));
    }

    @PutMapping(value = "/{spuId}/restoration", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "restoreProduct", summary = "还原回收站商品")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "商品状态", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本、状态或业务键冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<ProductStateResult> restoreProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @RequestBody @Validated(ProductValidationGroup.HttpStateInput.class) ProductLifecycleCommand command) {
        command.setSpuId(id(spuId));
        return ResultRecord.success(manage.restore(command));
    }

    @DeleteMapping("/{spuId}")
    @Operation(operationId = "deleteProduct", summary = "逻辑删除下架商品")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "操作成功", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本或状态冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<Boolean> deleteProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @Parameter(schema = @Schema(type = "integer", format = "int64", minimum = "0")) @RequestParam String version) {
        return ResultRecord.success(manage.delete(ProductLifecycleCommand.builder().spuId(id(spuId)).version(version(version)).build()));
    }

    @DeleteMapping("/{spuId}/purge")
    @Operation(operationId = "purgeProduct", summary = "永久删除回收站商品")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "操作成功", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "不存在或越租户", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError"))),
        @ApiResponse(responseCode = "409", description = "版本或状态冲突", content = @Content(schema = @Schema(ref = "#/components/schemas/ProductBusinessError")))})
    public ResultRecord<Boolean> purgeProduct(@Parameter(schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")) @PathVariable String spuId,
            @Parameter(schema = @Schema(type = "integer", format = "int64", minimum = "0")) @RequestParam String version) {
        return ResultRecord.success(manage.purge(ProductLifecycleCommand.builder().spuId(id(spuId)).version(version(version)).build()));
    }

    private static Long id(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) { throw invalid(); }
        try { return Long.parseLong(value); } catch (NumberFormatException failure) { throw invalid(); }
    }
    private static Long version(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]{0,18})")) { throw invalid(); }
        try { return Long.parseLong(value); } catch (NumberFormatException failure) { throw invalid(); }
    }
    private static CommonException invalid() { return new CommonException(GoodsErrorStatus.PRODUCT_REQUEST_INVALID); }
}
