package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.goods.domain.goods.FileDomainService;
import top.egon.enjoyshop.goods.domain.goods.ObjectStoragePort;
import top.egon.enjoyshop.goods.domain.goods.ProductImageBO;
import top.egon.enjoyshop.goods.domain.goods.ProductImageFormat;
import top.egon.enjoyshop.goods.domain.goods.ProductImageValidator;

@Service("fileDomainService")
@Validated
@Slf4j
@RequiredArgsConstructor
public class FileDomainServiceImpl implements FileDomainService {

    @Qualifier("s3CompatibleObjectStorageAdapter")
    private final ObjectStoragePort objectStoragePort;

    @Override
    public String upload(ProductImageBO image) {
        byte[] content = image.getContent();
        ProductImageFormat format = ProductImageValidator.detectFormat(content);
        String url = objectStoragePort.putObject(content, format);
        log.info("product image stored format={}", format);
        return url;
    }
}
