package top.egon.enjoyshop.goods.start.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(prefix = "app.object-storage")
public class ObjectStorageConfigurationProperties {

    @NotNull
    private URI endpoint;

    @NotBlank
    private String region;

    @NotBlank
    private String bucket;

    private boolean pathStyleAccess;

    @NotNull
    private URI publicUrlBase;

    @NotBlank
    @ToString.Exclude
    private String accessKey;

    @NotBlank
    @ToString.Exclude
    private String secretKey;

    @NotNull
    private Duration apiCallAttemptTimeout;

    @NotNull
    private Duration apiCallTimeout;

    @AssertTrue(message = "endpoint must be an absolute HTTP or HTTPS URI")
    public boolean isEndpointAbsolute() {
        return endpoint != null && endpoint.isAbsolute() && endpoint.getHost() != null
                && ("http".equalsIgnoreCase(endpoint.getScheme()) || "https".equalsIgnoreCase(endpoint.getScheme()));
    }

    @AssertTrue(message = "public URL base must be an absolute HTTPS URI")
    public boolean isPublicUrlBaseHttps() {
        return publicUrlBase != null && publicUrlBase.isAbsolute() && publicUrlBase.getHost() != null
                && "https".equalsIgnoreCase(publicUrlBase.getScheme());
    }

    @AssertTrue(message = "API call attempt timeout must be positive")
    public boolean isApiCallAttemptTimeoutPositive() {
        return apiCallAttemptTimeout != null && !apiCallAttemptTimeout.isZero() && !apiCallAttemptTimeout.isNegative();
    }

    @AssertTrue(message = "API call timeout must be at least the attempt timeout")
    public boolean isApiCallTimeoutAtLeastAttemptTimeout() {
        return apiCallTimeout != null && apiCallAttemptTimeout != null
                && !apiCallTimeout.isNegative() && apiCallTimeout.compareTo(apiCallAttemptTimeout) >= 0;
    }
}
