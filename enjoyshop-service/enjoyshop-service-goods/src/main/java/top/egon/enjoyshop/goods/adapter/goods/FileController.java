package top.egon.enjoyshop.goods.adapter.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.convertor.FileUploadAdapterConverter;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.FileUploadDTO;
import top.egon.enjoyshop.goods.application.goods.FileManage;
import top.egon.enjoyshop.goods.application.goods.FileUploadCommand;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

@RestController("fileController")
@RequestMapping("/api/v1/files")
@Validated
@Tag(name = "file", description = "商品图片文件")
@SecurityScheme(name = "shoubingBearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    @Qualifier("fileManage")
    private final FileManage manage;

    @Qualifier("fileUploadAdapterConverterImpl")
    private final FileUploadAdapterConverter converter;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "uploadFile", summary = "上传商品图片",
            security = @SecurityRequirement(name = "shoubingBearer"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "上传成功", headers =
            @Header(name = "Location", description = "稳定的公开图片 URL", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "缺失文件或图片无效"),
            @ApiResponse(responseCode = "401", description = "Shoubing Bearer 认证失败", content =
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object",
                    requiredProperties = {"code", "message"}, properties = {
                            @StringToClassMapItem(key = "code", value = String.class),
                            @StringToClassMapItem(key = "message", value = String.class)
                    }))),
            @ApiResponse(responseCode = "403", description = "Shoubing USER 未获准访问该资源", content =
            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object",
                    requiredProperties = {"code", "message"}, properties = {
                            @StringToClassMapItem(key = "code", value = String.class),
                            @StringToClassMapItem(key = "message", value = String.class)
                    }))),
            @ApiResponse(responseCode = "413", description = "文件超过 10 MiB"),
            @ApiResponse(responseCode = "503", description = "对象存储不可用或写入结果未知")
    })
    public ResponseEntity<ResultRecord<String>> uploadFile(
            @Valid @ModelAttribute
            @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = FileUploadDTO.class))) FileUploadDTO request) {
        if (request.getFile().getSize() > MAX_IMAGE_BYTES) {
            throw new CommonException(GoodsErrorStatus.FILE_TOO_LARGE);
        }

        FileUploadCommand command = converter.toTarget(request);
        String url = manage.upload(command);
        log.info("http product image upload completed");
        return ResponseEntity.created(URI.create(url)).body(ResultRecord.success(url));
    }
}
