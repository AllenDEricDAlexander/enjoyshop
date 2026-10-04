package top.egon.enjoyshop.goods.start.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration(value = "objectStorageConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(ObjectStorageConfigurationProperties.class)
@RequiredArgsConstructor
public class ObjectStorageConfiguration {

    @Qualifier("app.object-storage-top.egon.enjoyshop.goods.start.config.ObjectStorageConfigurationProperties")
    private final ObjectStorageConfigurationProperties properties;

    @Bean(value = "goodsS3Client", destroyMethod = "close")
    public S3Client goodsS3Client() {
        ClientOverrideConfiguration clientOverrides = ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(properties.getApiCallAttemptTimeout())
                .apiCallTimeout(properties.getApiCallTimeout())
                .retryStrategy(retry -> retry.maxAttempts(1))
                .build();
        return S3Client.builder()
                .endpointOverride(properties.getEndpoint())
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build())
                .overrideConfiguration(clientOverrides)
                .build();
    }
}
