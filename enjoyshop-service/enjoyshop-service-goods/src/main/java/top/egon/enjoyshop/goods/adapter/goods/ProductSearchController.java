package top.egon.enjoyshop.goods.adapter.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.application.search.ProductSearchManage;
import top.egon.enjoyshop.goods.application.search.ProductSearchPageResult;
import top.egon.enjoyshop.goods.application.search.ProductSearchQuery;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.enums.ProductSearchSortEnum;
import top.egon.enjoyshop.goods.common.validation.ProductSearchValidationGroup;

@RestController("productSearchController")
@RequestMapping(value = "/api/v1/product-search", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "product-search", description = "商城SKU搜索")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ProductSearchController {
    private static final Set<String> SCALARS = Set.of("keywords", "categoryId", "brandId", "minPrice",
            "maxPrice", "pageNo", "pageSize", "sort");
    @Qualifier("productSearchManage") private final ProductSearchManage manage;
    @Qualifier("egonColaValidationUtils") private final ValidationUtils validation;

    @GetMapping
    @SecurityRequirements
    @Operation(operationId = "searchProductSkus", summary = "检索商城商品SKU",
            description = "只读取站点所属租户的可售SKU。name词项AND、规格同键OR异键AND；候选使用全部当前过滤。"
                    + "分页从1开始，最大100行且窗口10000；价格为整数分，查询最终一致。"
                    + "spec.<name>允许重复string参数，客户端只编码一次；没有请求body或caller tenant。"
                    + "未接入搜索引擎时503，不返回假成功或空索引结果。")
    @Parameters({
        @Parameter(name = "keywords", description = "名称关键词；strip后最多100字符；空为浏览", schema = @Schema(type = "string", maxLength = 100, defaultValue = "")),
        @Parameter(name = "categoryId", description = "任一层分类ID", schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")),
        @Parameter(name = "brandId", description = "品牌ID", schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}")),
        @Parameter(name = "minPrice", description = "包含的最低价，整数分", schema = @Schema(type = "integer", format = "int32", minimum = "0", maximum = "2147483647")),
        @Parameter(name = "maxPrice", description = "包含的最高价，整数分，不小于minPrice", schema = @Schema(type = "integer", format = "int32", minimum = "0", maximum = "2147483647")),
        @Parameter(name = "pageNo", description = "1基页码，pageNo乘pageSize不超过10000", schema = @Schema(type = "integer", minimum = "1", maximum = "10000", defaultValue = "1")),
        @Parameter(name = "pageSize", description = "每页SKU数", schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20")),
        @Parameter(name = "sort", description = "固定排序编码，同值按SKU数值ID排序", schema = @Schema(type = "string", allowableValues = {"relevance", "priceAsc", "priceDesc", "salesDesc", "createdDesc"}, defaultValue = "relevance"))
    })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "完整SKU页、精确计数与严格候选；空页rows=[]", useReturnTypeSchema = true,
                headers = @Header(name = "Cache-Control", description = "no-store", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "610401：请求格式、字段、价格、规格或分页窗口非法", content = @Content(schema = @Schema(ref = "#/components/schemas/SearchRequestError"))),
        @ApiResponse(responseCode = "404", description = "610402：站点未登记或没有可信租户", content = @Content(schema = @Schema(ref = "#/components/schemas/SearchBusinessError"))),
        @ApiResponse(responseCode = "503", description = "610403：搜索关闭或引擎未接入/不可用", content = @Content(schema = @Schema(ref = "#/components/schemas/SearchBusinessError"))),
        @ApiResponse(responseCode = "500", description = "610499：安全内部错误", content = @Content(schema = @Schema(ref = "#/components/schemas/SearchBusinessError")))
    })
    public ResultRecord<ProductSearchPageResult> searchProductSkus(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parameters,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (request.getContentLengthLong() > 0) { throw invalid(); }
        Map<String, List<String>> specifications = new LinkedHashMap<>();
        parameters.forEach((key, values) -> {
            if (key.startsWith("spec.")) {
                specifications.put(key.substring(5), List.copyOf(values));
            } else if (!SCALARS.contains(key) || values.size() != 1) {
                throw invalid();
            }
        });
        ProductSearchQuery query = ProductSearchQuery.builder()
                .keywords(one(parameters, "keywords", "").strip())
                .categoryId(id(one(parameters, "categoryId", null)))
                .brandId(id(one(parameters, "brandId", null)))
                .minPrice(integer(one(parameters, "minPrice", null)))
                .maxPrice(integer(one(parameters, "maxPrice", null)))
                .pageNo(integer(one(parameters, "pageNo", "1")))
                .pageSize(integer(one(parameters, "pageSize", "20")))
                .sort(ProductSearchSortEnum.fromCode(one(parameters, "sort", "relevance")))
                .specs(specifications).build();
        validation.validate(query, ProductSearchValidationGroup.PublicSearch.class);
        return ResultRecord.success(manage.search(query));
    }

    private static String one(MultiValueMap<String, String> parameters, String key, String fallback) {
        return parameters.containsKey(key) ? parameters.getFirst(key) : fallback;
    }

    private static Long id(String value) {
        if (value == null) { return null; }
        if (!value.matches("[1-9][0-9]{0,18}")) { throw invalid(); }
        try { return Long.valueOf(value); } catch (NumberFormatException failure) { throw invalid(); }
    }

    private static Integer integer(String value) {
        if (value == null) { return null; }
        if (!value.matches("-?(?:0|[1-9][0-9]{0,9})")) { throw invalid(); }
        try { return Integer.valueOf(value); } catch (NumberFormatException failure) { throw invalid(); }
    }

    private static CommonException invalid() { return new CommonException(GoodsErrorStatus.SEARCH_REQUEST_INVALID); }
}
