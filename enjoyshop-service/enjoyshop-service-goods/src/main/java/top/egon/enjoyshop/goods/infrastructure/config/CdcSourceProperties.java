package top.egon.enjoyshop.goods.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Closed, secret-free allowlist and budgets for the single current PostgreSQL CDC source. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Validated
@ConfigurationProperties(prefix = "app.cdc", ignoreUnknownFields = false)
public class CdcSourceProperties {

    private boolean enabled;

    @Min(1)
    @Max(1_048_576)
    private int maxPayloadBytes = 1_048_576;

    @NotNull
    private Duration confirmTimeout = Duration.ofSeconds(5);

    @NotNull
    @Size(max = 1)
    private Map<@NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$") String,
            @Valid @NotNull SourceRegistration> sourceRegistry = Map.of();

    @AssertTrue(message = "enabled CDC requires one explicit master_data source and exact table allowlist")
    public boolean isRegistryValid() {
        if (!enabled) {
            return true;
        }
        if (sourceRegistry == null || sourceRegistry.size() != 1) {
            return false;
        }
        Set<String> sourceNames = new HashSet<>();
        Set<String> epochs = new HashSet<>();
        for (Map.Entry<String, SourceRegistration> entry : sourceRegistry.entrySet()) {
            SourceRegistration source = entry.getValue();
            if (!"master_data".equals(entry.getKey()) || source == null
                    || source.getSourceName() == null || !sourceNames.add(source.getSourceName())
                    || source.getSourceEpoch() == null || !epochs.add(source.getSourceEpoch())
                    || !"public".equals(source.getSchema())
                    || !Set.of("ad", "spu", "sku").equals(source.getTables())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "CDC confirmation timeout must be positive and no greater than five seconds")
    public boolean isConfirmTimeoutValid() {
        return confirmTimeout != null && !confirmTimeout.isZero() && !confirmTimeout.isNegative()
                && confirmTimeout.compareTo(Duration.ofSeconds(5)) <= 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class SourceRegistration {

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
        private String sourceName;

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
        private String sourceEpoch;

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
        private String topicPrefix;

        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9_]{0,62}$")
        private String database;

        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9_]{0,62}$")
        private String schema;

        @NotEmpty
        @Size(max = 3)
        private Set<@NotBlank @Pattern(regexp = "^(ad|spu|sku)$") String> tables;
    }
}
