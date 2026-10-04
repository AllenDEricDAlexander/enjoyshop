package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.domain.goods.ObjectStoragePort;
import top.egon.enjoyshop.goods.domain.goods.ProductImageFormat;
import top.egon.enjoyshop.goods.start.config.ObjectStorageConfigurationProperties;

@Component("s3CompatibleObjectStorageAdapter")
@Validated
@Slf4j
@RequiredArgsConstructor
public class S3CompatibleObjectStorageAdapter implements ObjectStoragePort {

    @Qualifier("goodsS3Client")
    private final S3Client s3Client;

    @Qualifier("app.object-storage-top.egon.enjoyshop.goods.start.config.ObjectStorageConfigurationProperties")
    private final ObjectStorageConfigurationProperties properties;

    @Override
    public String putObject(byte[] content, ProductImageFormat format) {
        String objectKey = UUID.randomUUID() + "." + format.getExtension();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(format.getContentType())
                .build();

        PutObjectResponse acknowledgement;
        try {
            acknowledgement = s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception exception) {
            log.warn("S3 object upload was rejected format={} status={}", format, exception.statusCode());
            throw new CommonException(GoodsErrorStatus.FILE_STORAGE_UNAVAILABLE);
        } catch (SdkClientException exception) {
            log.warn("S3 object upload outcome is unknown format={} failureType={}",
                    format, exception.getClass().getSimpleName());
            throw new CommonException(GoodsErrorStatus.FILE_STORAGE_OUTCOME_UNKNOWN);
        }

        if (acknowledgement == null) {
            log.warn("S3 object upload returned no acknowledgement format={}", format);
            throw new CommonException(GoodsErrorStatus.FILE_STORAGE_OUTCOME_UNKNOWN);
        }

        URI publicUrlBase = properties.getPublicUrlBase();
        String base = publicUrlBase.toString();
        URI normalizedBase = URI.create(base.endsWith("/") ? base : base + "/");
        String publicUrl = normalizedBase.resolve(objectKey).toString();
        log.info("S3 object upload acknowledged format={}", format);
        return publicUrl;
    }
}
