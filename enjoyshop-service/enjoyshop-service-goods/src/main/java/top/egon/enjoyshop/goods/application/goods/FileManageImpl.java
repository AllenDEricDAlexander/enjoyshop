package top.egon.enjoyshop.goods.application.goods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.goods.application.goods.converter.FileUploadDomainConverter;
import top.egon.enjoyshop.goods.domain.goods.FileDomainService;
import top.egon.enjoyshop.goods.domain.goods.ProductImageBO;

@Service("fileManage")
@Validated
@Slf4j
@RequiredArgsConstructor
public class FileManageImpl implements FileManage {

    @Qualifier("fileDomainService")
    private final FileDomainService fileDomainService;

    @Qualifier("fileUploadDomainConverterImpl")
    private final FileUploadDomainConverter converter;

    @Override
    public String upload(FileUploadCommand command) {
        ProductImageBO image = converter.toTarget(command);
        String url = fileDomainService.upload(image);
        log.info("product image upload completed");
        return url;
    }
}
