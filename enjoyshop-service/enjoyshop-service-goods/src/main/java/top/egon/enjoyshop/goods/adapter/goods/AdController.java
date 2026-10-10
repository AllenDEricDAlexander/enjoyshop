package top.egon.enjoyshop.goods.adapter.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
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
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.application.ad.AdLifecycleCommand;
import top.egon.enjoyshop.goods.application.ad.AdManage;
import top.egon.enjoyshop.goods.application.ad.AdPageQuery;
import top.egon.enjoyshop.goods.application.ad.AdPublicResult;
import top.egon.enjoyshop.goods.application.ad.AdReadQuery;
import top.egon.enjoyshop.goods.application.ad.AdResult;
import top.egon.enjoyshop.goods.application.ad.AdSaveCommand;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;

@RestController("adController")
@RequestMapping("/api/v1")
@Validated
@Tag(name = "ad", description = "广告管理与站点展示")
@RequiredArgsConstructor
@Slf4j
public class AdController {

    private static final String ADMIN_PATH = "/api/v1/ads";
    private static final Set<String> PAGE_QUERY_PARAMETERS = Set.of("pageNo", "pageSize", "position", "status");
    private static final Set<String> PUBLIC_QUERY_PARAMETERS = Set.of("limit");
    private static final Set<String> DELETE_QUERY_PARAMETERS = Set.of("version");

    @Qualifier("adManage")
    private final AdManage manage;

    @GetMapping("/ads")
    @Operation(operationId = "listAdvertisements", summary = "查询广告管理列表")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "查询参数不合法", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "需要已验证的用户身份", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "广告读取权限不足", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "503", description = "广告读取依赖不可用", useReturnTypeSchema = true)
    })
    public PageResultRecord<AdResult> listAdvertisements(
            HttpServletRequest request,
            @Parameter(description = "从 1 开始分页；缺省为 1", schema = @Schema(type = "integer", defaultValue = "1", minimum = "1", maximum = "1000000"))
            @RequestParam(name = "pageNo", required = false) String pageNo,
            @Parameter(description = "每页行数；缺省 20，最大 100", schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100"))
            @RequestParam(name = "pageSize", required = false) String pageSize,
            @Parameter(description = "精确过滤的已配置广告位", schema = @Schema(type = "string"))
            @RequestParam(name = "position", required = false) String position,
            @Parameter(description = "广告状态码：0 禁用，1 启用", schema = @Schema(type = "string", allowableValues = {"0", "1"}))
            @RequestParam(name = "status", required = false) String status) {
        rejectUnknownOrRepeated(request, PAGE_QUERY_PARAMETERS);
        AdPageQuery query = new AdPageQuery()
                .setPageNo(parseInteger(pageNo, 1, 1, 1_000_000))
                .setPageSize(parseInteger(pageSize, 20, 1, 100))
                .setPosition(position)
                .setStatus(parseStatus(status));
        return manage.page(query);
    }

    @GetMapping("/ads/{adId}")
    @Operation(operationId = "getAdvertisement", summary = "查询广告详情")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "广告详情", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "广告标识不合法", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "需要已验证的用户身份", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "广告读取权限不足", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "广告不存在或越租户", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "503", description = "广告读取依赖不可用", useReturnTypeSchema = true)
    })
    public ResultRecord<AdResult> getAdvertisement(
            @Parameter(description = "十进制字符串广告标识", schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}"))
            @PathVariable String adId) {
        return ResultRecord.success(manage.detail(parseId(adId)));
    }

    @PostMapping("/ads")
    @Operation(operationId = "createAdvertisement", summary = "新增广告")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "广告已创建",
                    headers = @Header(name = "Location", description = "新广告资源地址", schema = @Schema(type = "string")),
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "广告内容不合法", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "需要已验证的用户身份", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "广告写入权限不足", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "503", description = "广告写入依赖不可用", useReturnTypeSchema = true)
    })
    public ResponseEntity<ResultRecord<AdResult>> createAdvertisement(
            @RequestBody @Validated(AdValidationGroup.Create.class) AdSaveCommand command) {
        AdResult created = manage.create(command);
        log.info("http create advertisement id={}", created.getId());
        return ResponseEntity.created(URI.create(ADMIN_PATH + "/" + created.getId()))
                .body(ResultRecord.success(created));
    }

    @PutMapping("/ads/{adId}")
    @Operation(operationId = "replaceAdvertisement", summary = "完整替换广告内容与计划")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "广告已替换", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "广告内容或版本不合法", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "需要已验证的用户身份", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "广告写入权限不足", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "广告不存在或越租户", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "409", description = "广告版本已变化", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "503", description = "广告写入依赖不可用", useReturnTypeSchema = true)
    })
    public ResultRecord<AdResult> replaceAdvertisement(
            @Parameter(description = "十进制字符串广告标识", schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}"))
            @PathVariable String adId,
            @RequestBody @Validated(AdValidationGroup.Update.class) AdSaveCommand command) {
        return ResultRecord.success(manage.replace(parseId(adId), command));
    }

    @DeleteMapping("/ads/{adId}")
    @Operation(operationId = "deleteAdvertisement", summary = "软删除广告")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "广告标识或版本不合法", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "需要已验证的用户身份", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "广告写入权限不足", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "409", description = "广告版本已变化", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "503", description = "广告写入依赖不可用", useReturnTypeSchema = true)
    })
    public ResultRecord<Boolean> deleteAdvertisement(
            HttpServletRequest request,
            @Parameter(description = "十进制字符串广告标识", schema = @Schema(type = "string", pattern = "[1-9][0-9]{0,18}"))
            @PathVariable String adId,
            @Parameter(description = "最后读取的非负整数版本", schema = @Schema(type = "integer", format = "int64", minimum = "0"))
            @RequestParam(name = "version") String version) {
        rejectUnknownOrRepeated(request, DELETE_QUERY_PARAMETERS);
        AdLifecycleCommand command = new AdLifecycleCommand()
                .setId(parseId(adId))
                .setVersion(parseVersion(version));
        return ResultRecord.success(manage.delete(command));
    }

    @GetMapping("/ad-slots/{position}/ads")
    @Operation(operationId = "listPublishedAdvertisements", summary = "查询站点当前可展示广告")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "公开广告数组", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "广告位或limit不合法", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "站点未登记", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "503", description = "广告读取依赖不可用", useReturnTypeSchema = true)
    })
    public ResponseEntity<ResultRecord<List<AdPublicResult>>> listPublishedAdvertisements(
            HttpServletRequest request,
            @Parameter(description = "受配置控制的广告位", schema = @Schema(type = "string"))
            @PathVariable String position,
            @Parameter(description = "最多返回 20 条，缺省 20", schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "20"))
            @RequestParam(name = "limit", required = false) String limit) {
        rejectUnknownOrRepeated(request, PUBLIC_QUERY_PARAMETERS);
        AdReadQuery query = new AdReadQuery()
                .setPosition(position)
                .setLimit(parseInteger(limit, 20, 1, 20));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResultRecord.success(manage.published(query)));
    }

    private static void rejectUnknownOrRepeated(HttpServletRequest request, Set<String> allowedNames) {
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            String[] values = parameter.getValue();
            if (!allowedNames.contains(parameter.getKey()) || values == null || values.length != 1) {
                throw invalidRequest();
            }
        }
    }

    private static int parseInteger(String value, int defaultValue, int minimum, int maximum) {
        if (value == null) {
            return defaultValue;
        }
        if (!value.matches("(?:0|[1-9][0-9]{0,9})")) {
            throw invalidRequest();
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw invalidRequest();
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw invalidRequest();
        }
    }

    private static Long parseId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw invalidRequest();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException failure) {
            throw invalidRequest();
        }
    }

    private static Long parseVersion(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]{0,18})")) {
            throw invalidRequest();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException failure) {
            throw invalidRequest();
        }
    }

    private static AdStatusEnum parseStatus(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "0" -> AdStatusEnum.DISABLED;
            case "1" -> AdStatusEnum.ENABLED;
            default -> throw invalidRequest();
        };
    }

    private static CommonException invalidRequest() {
        return new CommonException(GoodsErrorStatus.AD_REQUEST_INVALID);
    }
}
