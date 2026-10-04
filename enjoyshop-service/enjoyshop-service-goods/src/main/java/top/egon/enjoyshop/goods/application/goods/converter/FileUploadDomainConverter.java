package top.egon.enjoyshop.goods.application.goods.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.application.goods.FileUploadCommand;
import top.egon.enjoyshop.goods.domain.goods.ProductImageBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FileUploadDomainConverter extends BaseForwardConverter<FileUploadCommand, ProductImageBO> {

    @Override
    @Mapping(target = "content", source = "content")
    ProductImageBO toTarget(FileUploadCommand source);
}
