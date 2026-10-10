package top.egon.enjoyshop.goods.domain.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Row identity facts extracted at the raw-adapter boundary; never contains a vendor JSON tree. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class CdcRecordBO {

    @Valid
    @NotNull
    private CdcSourceCursorBO cursor;

    @Pattern(regexp = "^[a-f0-9]{64}$")
    @NotNull
    private String factFingerprint;

    @Positive
    private Long beforeRowId;

    @Positive
    private Long beforeTenantId;

    @Positive
    private Long beforeSpuId;

    @Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$")
    private String beforePosition;

    @Positive
    private Long afterRowId;

    @Positive
    private Long afterTenantId;

    @Positive
    private Long afterSpuId;

    @Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$")
    private String afterPosition;

    @AssertTrue(message = "row identity sides must match the operation and source table")
    public boolean isChangeIdentityValid() {
        if (cursor == null || cursor.getOperation() == null || cursor.getTable() == null) {
            return true;
        }
        boolean beforeRequired = cursor.getOperation() == CdcOperationEnum.UPDATE
                || cursor.getOperation() == CdcOperationEnum.DELETE;
        boolean afterRequired = cursor.getOperation() == CdcOperationEnum.UPDATE
                || cursor.getOperation() == CdcOperationEnum.CREATE
                || cursor.getOperation() == CdcOperationEnum.SNAPSHOT;
        return sideIsValid(true, beforeRequired) && sideIsValid(false, afterRequired);
    }

    @AssertTrue(message = "row ids must match the PostgreSQL source primary key")
    public boolean isSourcePrimaryKeyConsistent() {
        if (cursor == null || cursor.getRowPk() == null) {
            return true;
        }
        return (beforeRowId == null || Objects.equals(beforeRowId, cursor.getRowPk()))
                && (afterRowId == null || Objects.equals(afterRowId, cursor.getRowPk()));
    }

    private boolean sideIsValid(boolean before, boolean required) {
        Long rowId = before ? beforeRowId : afterRowId;
        Long tenantId = before ? beforeTenantId : afterTenantId;
        Long spuId = before ? beforeSpuId : afterSpuId;
        String position = before ? beforePosition : afterPosition;
        boolean allAbsent = rowId == null && tenantId == null && spuId == null && position == null;
        if (!required) {
            return allAbsent;
        }
        if (rowId == null || tenantId == null) {
            return false;
        }
        return switch (cursor.getTable()) {
            case "ad" -> position != null && spuId == null;
            case "spu" -> position == null && spuId == null;
            case "sku" -> position == null && spuId != null;
            default -> false;
        };
    }
}
