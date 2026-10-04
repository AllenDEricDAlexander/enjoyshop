package top.egon.enjoyshop.goods.start.config;

import jakarta.validation.Validation;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageConfigurationPropertiesTest {

    @Test
    void valid_config_binds_typed_endpoint_credentials_and_timeouts() {
        ObjectStorageConfigurationProperties properties = bind(validValues());

        assertThat(properties.getEndpoint().toString()).isEqualTo("http://localhost:9000");
        assertThat(properties.getRegion()).isEqualTo("us-east-1");
        assertThat(properties.getBucket()).isEqualTo("product-media");
        assertThat(properties.isPathStyleAccess()).isTrue();
        assertThat(properties.getPublicUrlBase().toString()).isEqualTo("https://images.example.test/products/");
        assertThat(properties.getApiCallAttemptTimeout()).hasSeconds(15);
        assertThat(properties.getApiCallTimeout()).hasSeconds(20);
    }

    @Test
    void required_endpoint_credentials_public_base_and_timeouts_are_validated() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            ObjectStorageConfigurationProperties properties = bind(Map.of());

            assertThat(factory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("endpoint", "region", "bucket", "publicUrlBase", "accessKey", "secretKey",
                            "apiCallAttemptTimeout", "apiCallTimeout");
        }
    }

    @Test
    void total_timeout_cannot_be_shorter_than_attempt_timeout() {
        Map<String, String> values = validValues();
        values.put("app.object-storage.api-call-timeout", "10s");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(bind(values))).isNotEmpty();
        }
    }

    @Test
    void endpoint_must_be_absolute_http_and_public_url_base_must_be_absolute_https() {
        Map<String, String> values = validValues();
        values.put("app.object-storage.endpoint", "relative-endpoint");
        values.put("app.object-storage.public-url-base", "http://images.example.test/products/");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(bind(values)))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("endpointAbsolute", "publicUrlBaseHttps");
        }
    }

    @Test
    void to_string_redacts_storage_credentials() {
        ObjectStorageConfigurationProperties properties = bind(validValues());

        assertThat(properties.toString()).doesNotContain("test-access-key", "test-secret-key");
    }

    private static ObjectStorageConfigurationProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bindOrCreate("app.object-storage", Bindable.of(ObjectStorageConfigurationProperties.class));
    }

    private static Map<String, String> validValues() {
        Map<String, String> values = new HashMap<>();
        values.put("app.object-storage.endpoint", "http://localhost:9000");
        values.put("app.object-storage.region", "us-east-1");
        values.put("app.object-storage.bucket", "product-media");
        values.put("app.object-storage.path-style-access", "true");
        values.put("app.object-storage.public-url-base", "https://images.example.test/products/");
        values.put("app.object-storage.access-key", "test-access-key");
        values.put("app.object-storage.secret-key", "test-secret-key");
        values.put("app.object-storage.api-call-attempt-timeout", "15s");
        values.put("app.object-storage.api-call-timeout", "20s");
        return values;
    }
}
