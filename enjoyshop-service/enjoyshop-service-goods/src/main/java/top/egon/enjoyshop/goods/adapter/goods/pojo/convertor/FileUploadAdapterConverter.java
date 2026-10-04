package top.egon.enjoyshop.goods.adapter.goods.pojo.convertor;

import java.io.IOException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.multipart.MultipartFile;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.FileUploadDTO;
import top.egon.enjoyshop.goods.application.goods.FileUploadCommand;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FileUploadAdapterConverter extends BaseForwardConverter<FileUploadDTO, FileUploadCommand> {

    @Override
    @Mapping(target = "content", source = "file", qualifiedByName = "multipartContent")
    FileUploadCommand toTarget(FileUploadDTO source);

    @Named("multipartContent")
    static byte[] multipartContent(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new CommonException(GoodsErrorStatus.FILE_REQUEST_INVALID);
        }
    }
}
