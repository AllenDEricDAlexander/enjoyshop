package top.egon.enjoyshop.goods.application.goods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
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

    @Qualifier("fileAuthorizationComponent")
    private final FileAuthorizationComponent authorization;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Override
    public String upload(FileUploadCommand command) {
        authorization.requirePermission("enjoyshop:file:write");
        validation.validate(command);
        ProductImageBO image = converter.toTarget(command);
        String url = fileDomainService.upload(image);
        log.info("product image upload completed");
        return url;
    }
}
