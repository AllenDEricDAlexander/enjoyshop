# PostgreSQL replication and CDC source contract

This document separates physical replication, application routing, and logical CDC. The application writes and transactional reads use the configured PRIMARY pools; ordinary non-transactional reads may use replicas. Debezium connects only to the PostgreSQL PRIMARY that owns `public.ad`, `public.spu`, and `public.sku`. A physical standby is not a CDC source and does not receive Rabbit messages.

Before release, the DBA and platform owner must fill this mapping with actual infrastructure evidence. One PostgreSQL cluster may host more than one logical JDBC database; do not infer a cluster boundary from an application datasource name.

| Input | Required evidence | Current value |
| --- | --- | --- |
| PostgreSQL major and patch | `SHOW server_version` from PRIMARY and standby | Not provided |
| Cluster/database map | logical group → cluster identity → database → PRIMARY/standby addresses | Not provided |
| Physical replication | synchronous/asynchronous mode, replication slots, replay lag, failover owner | Not provided |
| RPO and WAL budget | approved outage window and peak WAL rate × outage window + safety margin | Not provided |
| CDC role | LOGIN/REPLICATION, database CONNECT, SELECT on captured tables, TLS identity | Not provided |
| Publication and slot | exact publication, unique slot, source name, epoch and offset continuity | Not provided |
| Recovery owner | DBA primary fencing, promotion, slot/offset validation and new-epoch approval | Not provided |

The goods migration creates only the new advertisement object and its approved `REPLICA IDENTITY FULL` settings. The DBA must create and verify a publication containing exactly `public.ad`, `public.spu`, and `public.sku`. The Server template sets `debezium.source.publication.autocreate.mode=disabled`, so it cannot create or expand the publication at startup. Do not grant the application or Server permission to execute schema changes as a substitute for this gate.

Read-only checks for the DBA to run against the actual PRIMARY include:

```sql
SELECT pg_is_in_recovery();
SHOW wal_level;
SELECT slot_name, slot_type, database, active, restart_lsn, confirmed_flush_lsn
FROM pg_replication_slots;
SELECT pubname, schemaname, tablename
FROM pg_publication_tables
WHERE pubname = '<approved_publication>'
ORDER BY schemaname, tablename;
SELECT relname, relreplident
FROM pg_class
WHERE relnamespace = 'public'::regnamespace
  AND relname IN ('ad', 'spu', 'sku')
ORDER BY relname;
```

These are evidence prompts only. This task did not connect to PostgreSQL or execute SQL. The expected CDC publication has the three rows above and the three captured tables report `relreplident = 'f'` after the managed migration is applied. Confirm the active connector is on the writable PRIMARY and that `restart_lsn`, `confirmed_flush_lsn`, retained WAL, and standby replay lag stay within the DBA-approved capacity budget.

There is one active owner per logical slot. Use a single-writer persistent offset volume and a controlled `Recreate` update for the independent Server. A Kubernetes lease or replica count is not proof that the former PRIMARY is fenced. During failover, first fence the old PRIMARY, then prove physical replication, slot and offset continuity before restarting the Server. Only use PostgreSQL 17+ failover slots when the exact PostgreSQL and Debezium releases meet all documented prerequisites and the DBA has verified the synchronized slot and WAL positions. For any lost slot, expired WAL, missing offset, rewind, or unprovable continuity, stop CDC, allocate a new `sourceEpoch`, and perform a controlled initial snapshot and full downstream reconciliation. Never reset offset state or run Flyway `repair` to disguise a discontinuity.

The publication changes only what the connector observes. Direct SQL and service writes both create committed WAL facts; rolled-back transactions must not produce row events. `REPLICA IDENTITY FULL` is required so deletes and old/new ownership moves retain the before image used for tenant-safe advertisement invalidation and SPU refresh.
