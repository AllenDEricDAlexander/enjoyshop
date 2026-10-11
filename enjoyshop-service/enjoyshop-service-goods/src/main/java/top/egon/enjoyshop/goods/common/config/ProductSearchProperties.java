package top.egon.enjoyshop.goods.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Offline phase configuration; ES endpoints and client credentials are intentionally not provisioned here. */
@Component("productSearchProperties")
@ConfigurationProperties(prefix = "app.search", ignoreUnknownFields = false)
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class ProductSearchProperties {
    private boolean enabled;
    @NotNull @Size(max = 100) @Builder.Default
    private Map<@Pattern(regexp = "^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$") String,
            @NotNull @Min(1) Long> publicSites = Map.of();
    @NotNull @Builder.Default private Duration queryTimeout = Duration.ofSeconds(2);

    @AssertTrue(message = "Enabled search requires registered public sites")
    public boolean isSiteConfigurationValid() { return !enabled || (publicSites != null && !publicSites.isEmpty()); }

    @AssertTrue(message = "Search timeout must be positive and no greater than two seconds")
    public boolean isQueryTimeoutValid() {
        return queryTimeout != null && !queryTimeout.isZero() && !queryTimeout.isNegative()
                && queryTimeout.compareTo(Duration.ofSeconds(2)) <= 0;
    }
}
