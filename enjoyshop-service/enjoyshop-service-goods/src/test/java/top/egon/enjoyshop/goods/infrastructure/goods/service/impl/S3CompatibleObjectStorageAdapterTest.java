package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.domain.goods.ProductImageFormat;
import top.egon.enjoyshop.goods.start.config.ObjectStorageConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3CompatibleObjectStorageAdapterTest {

    private static final String PUBLIC_URL_BASE = "https://images.example.test/product-media/";

    private static final byte[] CONTENT = {1, 2, 3, 4, 5};

    @Mock
    private S3Client s3Client;

    private S3CompatibleObjectStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new S3CompatibleObjectStorageAdapter(s3Client, properties());
    }

    @Test
    void acknowledged_put_uses_detected_format_and_returns_stable_public_url() throws IOException {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String url = adapter.putObject(CONTENT, ProductImageFormat.JPEG);

        assertThat(url).startsWith(PUBLIC_URL_BASE);
        String key = url.substring(PUBLIC_URL_BASE.length());
        assertThat(key).matches("[0-9a-f-]{36}\\.jpg");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(1)).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("product-media");
        assertThat(request.getValue().key()).isEqualTo(key);
        assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(body.getValue().contentLength()).isEqualTo(CONTENT.length);
        try (InputStream stream = body.getValue().contentStreamProvider().newStream()) {
            assertThat(stream.readAllBytes()).containsExactly(CONTENT);
        }
    }

    @Test
    void definite_s3_rejection_maps_to_storage_unavailable_without_retry() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(503).message("storage rejected request").build());

        assertThatThrownBy(() -> adapter.putObject(CONTENT, ProductImageFormat.PNG))
                .isInstanceOfSatisfying(CommonException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GoodsErrorStatus.FILE_STORAGE_UNAVAILABLE.getCode()));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void client_failure_maps_to_unknown_outcome_without_retry_or_url() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("simulated timeout"));

        assertThatThrownBy(() -> adapter.putObject(CONTENT, ProductImageFormat.PNG))
                .isInstanceOfSatisfying(CommonException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GoodsErrorStatus.FILE_STORAGE_OUTCOME_UNKNOWN.getCode()));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void missing_put_acknowledgement_maps_to_unknown_outcome() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        assertThatThrownBy(() -> adapter.putObject(CONTENT, ProductImageFormat.PNG))
                .isInstanceOfSatisfying(CommonException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(GoodsErrorStatus.FILE_STORAGE_OUTCOME_UNKNOWN.getCode()));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    private static ObjectStorageConfigurationProperties properties() {
        return ObjectStorageConfigurationProperties.builder()
                .endpoint(URI.create("http://s3.example.test"))
                .region("us-east-1")
                .bucket("product-media")
                .pathStyleAccess(true)
                .publicUrlBase(URI.create(PUBLIC_URL_BASE))
                .accessKey("test-access-key")
                .secretKey("test-secret-key")
                .apiCallAttemptTimeout(Duration.ofSeconds(15))
                .apiCallTimeout(Duration.ofSeconds(20))
                .build();
    }
}
