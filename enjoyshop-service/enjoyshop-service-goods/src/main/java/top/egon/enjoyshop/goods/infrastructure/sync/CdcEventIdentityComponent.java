package top.egon.enjoyshop.goods.infrastructure.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.domain.sync.CdcRecordBO;
import top.egon.enjoyshop.goods.domain.sync.CdcSourceCursorBO;

/** Stable identities are derived only from committed source facts and their semantic target. */
@Component("cdcEventIdentityComponent")
@Validated
@RequiredArgsConstructor
@Slf4j
public class CdcEventIdentityComponent {

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    public String eventId(CdcRecordBO record, Long targetTenant, String routingKey) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(targetTenant, "targetTenant");
        Objects.requireNonNull(routingKey, "routingKey");
        CdcSourceCursorBO cursor = Objects.requireNonNull(record.getCursor(), "cursor");
        TreeMap<String, Object> canonical = new TreeMap<>();
        canonical.put("sourceAlias", cursor.getSourceAlias());
        canonical.put("sourceEpoch", cursor.getSourceEpoch());
        canonical.put("schema", cursor.getSchema());
        canonical.put("table", cursor.getTable());
        canonical.put("operation", cursor.getOperation() == null ? null : cursor.getOperation().getCode());
        canonical.put("rowPk", cursor.getRowPk());
        canonical.put("sourceLsn", cursor.getSourceLsn() == null ? null : cursor.getSourceLsn().toString());
        canonical.put("sourceTxId", cursor.getSourceTxId());
        canonical.put("sourceRecordOrder", cursor.getSourceRecordOrder());
        canonical.put("sourceTsMs", cursor.getSourceTsMs());
        canonical.put("beforeRowId", record.getBeforeRowId());
        canonical.put("beforeTenantId", record.getBeforeTenantId());
        canonical.put("beforeSpuId", record.getBeforeSpuId());
        canonical.put("beforePosition", record.getBeforePosition());
        canonical.put("afterRowId", record.getAfterRowId());
        canonical.put("afterTenantId", record.getAfterTenantId());
        canonical.put("afterSpuId", record.getAfterSpuId());
        canonical.put("afterPosition", record.getAfterPosition());
        canonical.put("beforeAfterFactHash", record.getFactFingerprint());
        canonical.put("targetTenant", targetTenant);
        canonical.put("routingKey", routingKey);
        return sha256(serialize(canonical));
    }

    public String contentFingerprint(CdcChangeStrategy.SyncEvent event) {
        Objects.requireNonNull(event, "event");
        try {
            return sha256(objectMapper.writeValueAsBytes(event));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("CDC event could not be canonicalized", failure);
        }
    }

    private byte[] serialize(TreeMap<String, Object> canonical) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            canonical.forEach((name, value) -> node.set(name, objectMapper.valueToTree(value)));
            return objectMapper.writeValueAsBytes(node);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("CDC source facts could not be canonicalized", failure);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
