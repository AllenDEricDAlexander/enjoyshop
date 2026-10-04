package top.egon.enjoyshop.goods.adapter.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.convertor.SpecAdapterConverter;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.SpecCategoryQueryDTO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.SpecOptionVO;
import top.egon.enjoyshop.goods.application.goods.SpecListQuery;
import top.egon.enjoyshop.goods.application.goods.SpecManage;
import top.egon.enjoyshop.goods.application.goods.SpecResult;
import top.egon.enjoyshop.goods.common.validation.ValidCategoryName;

@RestController("specController")
@RequestMapping("/api/v1/specs")
@Validated
@Tag(name = "spec", description = "商品规格字典")
@RequiredArgsConstructor
@Slf4j
public class SpecController {

    @Qualifier("specManage")
    private final SpecManage manage;

    @Qualifier("specAdapterConverterImpl")
    private final SpecAdapterConverter converter;

    @GetMapping("/category/{categoryName}")
    @Operation(operationId = "listSpecsByCategoryName", summary = "按分类查询规格选项")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "分类名称不合法，业务码 610110"),
            @ApiResponse(responseCode = "500", description = "服务内部错误")
    })
    public ResultRecord<List<SpecOptionVO>> listSpecsByCategoryName(
            @Parameter(description = "商品分类名称", required = true,
                    schema = @Schema(type = "string", minLength = 1, maxLength = 120))
            @ValidCategoryName(required = true)
            @NotNull
            @PathVariable("categoryName") String categoryName) {
        SpecCategoryQueryDTO request = new SpecCategoryQueryDTO().setCategoryName(categoryName);
        SpecListQuery query = converter.toListQuery(request);
        List<SpecResult> results = manage.listSpecsByCategoryName(query);
        return ResultRecord.success(converter.toVOList(results));
    }
}
