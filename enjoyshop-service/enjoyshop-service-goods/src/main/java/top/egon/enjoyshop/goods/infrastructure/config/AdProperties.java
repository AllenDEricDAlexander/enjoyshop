package top.egon.enjoyshop.goods.infrastructure.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(prefix = "app.ad", ignoreUnknownFields = false)
public class AdProperties {

    private boolean publicEnabled;

    @NotNull
    @Size(max = 100)
    @Builder.Default
    private Map<@Pattern(regexp = "^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$") String,
            @NotNull @Min(1) Long> publicSites = Map.of();

    @NotNull
    @Size(max = 100)
    @Builder.Default
    private List<@Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$") String> allowedPositions = List.of();

    @Min(1)
    @Max(200)
    @Builder.Default
    private int candidateLimit = 200;

    @Min(1)
    @Max(20)
    @Builder.Default
    private int publicLimit = 20;

    @NotNull
    @Builder.Default
    private Duration loadTimeout = Duration.ofSeconds(2);

    @AssertTrue(message = "public advertisement reads require registered sites and positions")
    public boolean isPublicConfigurationReady() {
        return !publicEnabled || (publicSites != null && !publicSites.isEmpty()
                && allowedPositions != null && !allowedPositions.isEmpty());
    }

    @AssertTrue(message = "advertisement positions must be unique and the public limit must not exceed candidate limit")
    public boolean isConfigurationConsistent() {
        return allowedPositions != null
                && new HashSet<>(allowedPositions).size() == allowedPositions.size()
                && publicLimit <= candidateLimit;
    }

    @AssertTrue(message = "advertisement load timeout must be positive and no greater than two seconds")
    public boolean isLoadTimeoutValid() {
        return loadTimeout != null && !loadTimeout.isZero() && !loadTimeout.isNegative()
                && loadTimeout.compareTo(Duration.ofSeconds(2)) <= 0;
    }
}
