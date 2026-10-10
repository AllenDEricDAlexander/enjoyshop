package top.egon.enjoyshop.goods.domain.sync;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Versioned Rabbit request to reread current tenant-scoped product state. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"eventId", "schemaVersion", "tenantId", "sourceAlias", "sourceEpoch", "sourceLsn",
        "sourceTxId", "sourceRecordOrder", "occurredAt", "sourceTable", "operation", "spuIds"})
public class ProductRefreshRequestedEvent implements CdcChangeStrategy.SyncEvent {

    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String eventId;

    @Min(1)
    @Max(1)
    private Integer schemaVersion;

    @NotNull
    @Positive
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$")
    private String sourceAlias;

    @NotBlank
    @Size(max = 128)
    private String sourceEpoch;

    @Pattern(regexp = "^(?:0|[1-9A-F][0-9A-F]*)/[0-9A-F]{8}$")
    private String sourceLsn;

    @Pattern(regexp = "^(0|[1-9][0-9]*)$")
    private String sourceTxId;

    @Pattern(regexp = "^[1-9][0-9]*$")
    private String sourceRecordOrder;

    @NotNull
    private Instant occurredAt;

    @NotBlank
    @Pattern(regexp = "^(spu|sku)$")
    private String sourceTable;

    @NotNull
    private CdcOperationEnum operation;

    @NotEmpty
    @Size(max = 2)
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<@NotNull @Positive Long> spuIds;

    public ProductRefreshRequestedEvent setSpuIds(List<Long> spuIds) {
        this.spuIds = spuIds == null ? null : List.copyOf(spuIds);
        return this;
    }

    @AssertTrue(message = "SPU roots must be sorted and unique")
    @JsonIgnore
    public boolean isSpuIdsCanonical() {
        return spuIds == null || spuIds.stream().distinct().count() == spuIds.size()
                && spuIds.equals(spuIds.stream().sorted().toList());
    }

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unknown product refresh event field: " + name);
    }

    public static class ProductRefreshRequestedEventBuilder {
        public ProductRefreshRequestedEventBuilder spuIds(List<Long> spuIds) {
            this.spuIds = spuIds == null ? null : List.copyOf(spuIds);
            return this;
        }
    }
}
