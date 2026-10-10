package top.egon.enjoyshop.goods.domain.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Strategy contract for table-specific old/new target calculation. */
public interface CdcChangeStrategy {

    boolean supports(String sourceAlias, String schema, String table);

    String routingKey();

    List<? extends SyncEvent> normalize(@Valid @NotNull CdcRecordBO record);

    interface SyncEvent {
        String getEventId();

        Integer getSchemaVersion();

        Long getTenantId();

        String getSourceAlias();

        String getSourceEpoch();

        String getSourceLsn();

        String getSourceTxId();

        String getSourceRecordOrder();

        Instant getOccurredAt();

        String getSourceTable();

        CdcOperationEnum getOperation();
    }
}
