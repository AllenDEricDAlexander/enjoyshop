package top.egon.enjoyshop.goods.domain.sync;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Exact source coordinates retained from one committed PostgreSQL row fact. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class CdcSourceCursorBO {

    private static final BigInteger MAX_LSN = new BigInteger("18446744073709551615");
    private static final BigInteger LOW_LSN_MASK = new BigInteger("FFFFFFFF", 16);

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$")
    private String sourceAlias;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
    private String sourceEpoch;

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,62}$")
    private String schema;

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{0,62}$")
    private String table;

    @NotNull
    private CdcOperationEnum operation;

    @NotNull
    @Positive
    private Long rowPk;

    @DecimalMin("0")
    @DecimalMax("18446744073709551615")
    @Digits(integer = 20, fraction = 0)
    private BigInteger sourceLsn;

    @Size(max = 20)
    @Pattern(regexp = "^(0|[1-9][0-9]*)$")
    private String sourceTxId;

    @Size(max = 20)
    @Pattern(regexp = "^[1-9][0-9]*$")
    private String sourceRecordOrder;

    @NotNull
    @PositiveOrZero
    private Long sourceTsMs;

    @AssertTrue(message = "source transaction metadata must be complete")
    public boolean isTransactionMetadataConsistent() {
        return sourceRecordOrder == null || sourceTxId != null;
    }

    @AssertTrue(message = "live source operations require an LSN")
    public boolean isSourceLsnValid() {
        return operation == null || operation == CdcOperationEnum.SNAPSHOT || sourceLsn != null;
    }

    @AssertTrue(message = "source LSN must fit PostgreSQL's unsigned 64-bit cursor")
    public boolean isSourceLsnInRange() {
        return sourceLsn == null || sourceLsn.signum() >= 0 && sourceLsn.compareTo(MAX_LSN) <= 0;
    }

    public Instant getOccurredAt() {
        return sourceTsMs == null ? null : Instant.ofEpochMilli(sourceTsMs);
    }

    public String getSourceLsnText() {
        if (sourceLsn == null) {
            return null;
        }
        String high = sourceLsn.shiftRight(32).toString(16).toUpperCase(Locale.ROOT);
        String low = sourceLsn.and(LOW_LSN_MASK).toString(16).toUpperCase(Locale.ROOT);
        return high + "/" + "0".repeat(8 - low.length()) + low;
    }
}
