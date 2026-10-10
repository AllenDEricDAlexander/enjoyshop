# PostgreSQL CDC release acceptance record

This page is an evidence form for a later environment owner. Every row starts as **Not run**. The deployment release is blocked until every required input and TEST-019–022 evidence is recorded. Offline tests validate the application and configuration contracts; they do not establish live PostgreSQL, RabbitMQ, Redis, Debezium Server, failover, or Kubernetes readiness.

## Release inputs

| Input | Required value/evidence | Status |
| --- | --- | --- |
| PostgreSQL versions and PRIMARY/standby map | major/patch, cluster/database mapping, role verification, TLS endpoint | Not provided |
| Debezium Server release | exact version, distribution artifact, connector module versions, immutable image digest | Not provided |
| RabbitMQ release and vhost | broker version, vhost, TLS settings, quorum feature/dead-letter capability | Not provided |
| Kubernetes target | cluster version, namespace, domain, storage class, PVC capacity, NetworkPolicy owner | Not provided |
| CDC source registration | `source.name`, topic prefix, database, publication, slot, current `sourceEpoch` | Not provided |
| Recovery objectives | approved async replication RPO, maximum outage, WAL capacity and alarm thresholds | Not provided |
| Application release inputs | site→tenant map, allowed ad positions, RBAC grants, Redis topology | Not provided |
| Search owner | backlog capacity/retention and next-phase full-rebuild/fence owner | Not provided |

The Debezium property names in `debezium/application.properties` were checked against the official Debezium Server 3.2 and PostgreSQL connector references. That reference family is not a selected or pinned runtime artifact. Resolve the exact server distribution, connector modules, Rabbit client behavior, and image digest before deployment. The Rabbit vhost/TLS pass-through must be verified against that exact artifact rather than copied from a different release.

## TEST-019 — Live PostgreSQL routing and persistence

Status: **Not run**. Record the UTC run time, PostgreSQL version, environment, command/operator, and attached evidence.

- Verify the three logical groups use the approved PRIMARY and replica connections, the full native rule resource loads, all six physical targets are present, and existing managed DDL history/fingerprints match without repair.
- Confirm writes and explicit read-only/repeatable-read transactions reach PRIMARY. With a deliberately lagging replica, verify ordinary replica reads may be stale while an advertisement cache miss/refill still reads PRIMARY.
- Validate `ad`, `spu`, and `sku` have `REPLICA IDENTITY FULL`, the approved publication contains only those tables, and the logical slot is active on the actual PRIMARY.
- Exercise the managed migration through the normal DBA/Common process, audit timestamp precision and JSON/ID values, and capture `EXPLAIN` plus role evidence. This Plan execution did not run that process.

Evidence command/output, UTC timestamp, owner, and result:

```text
Not run — environment details and DBA evidence have not been supplied.
```

## TEST-020 — Primary fencing, promotion, and slot/offset continuity

Status: **Not run**. Do not promote a server or move a running task as part of this preparation.

- Capture the approved physical replication mode and RPO. Fence the former PRIMARY before any promotion.
- For a continuity claim, compare the promoted server's synchronized slot positions, Debezium persistent offset, WAL availability, and source epoch against the exact release prerequisites. PostgreSQL 17+ alone is not sufficient evidence.
- If slot/WAL/offset continuity cannot be proven, keep the bridge stopped, assign a new epoch, and perform a controlled initial snapshot and full reconciliation. Never reset offsets or discard DLQ messages to force recovery.

Evidence command/output, UTC timestamp, owner, and result:

```text
Not run — primary fencing and failover evidence are not available.
```

## TEST-021 — Committed-row capture, snapshots, and restart/replay

Status: **Not run**. Use an approved isolated data set after the release matrix and publication are accepted.

- Verify `c/u/d/r`, before/after, LSN, transaction ID/order, schema-disabled JSON values, old/new position moves, SPU changes, SKU parent moves, tenant moves, hard deletes, and TOAST behavior.
- Confirm a rolled-back transaction produces no committed row event, while a direct authorized SQL update does. An update to an unprojected column still produces a refresh fact; it is not filtered as unchanged.
- Stop/restart the one active Server using the persistent offset volume. Replay a confirmed raw message and verify stable source identity/event IDs and duplicate-safe cache invalidation.
- Verify WAL retention, slot lag, and recovery alarms at normal load and under the approved outage window.

Evidence command/output, UTC timestamp, owner, and result:

```text
Not run — Debezium Server, PostgreSQL, and persistent offset storage are not configured.
```

## TEST-022 — RabbitMQ, Redis, and quarantine behavior

Status: **Not run**. Use the exact broker/client/Redis versions recorded in the release matrix.

- Confirm persistent publishes, correlated confirms, returns, fixed bindings, alternate-exchange routing, quorum dead-letter policies, `overflow=reject-publish`, ACLs, and bounded retry/manual reject.
- Remove one test binding and prove the message reaches the unrouted quarantine. Inject malformed source/schema data and confirm durable isolation with the original payload and identity.
- Stop semantic consumers and observe queue age/capacity alarms. Prove the search queue remains unconsumed in this phase.
- With two goods/cache nodes, verify `EgonColaCachePort` eviction events and measure the actual L2 behavior. The component may swallow Redis failures; a returned SPI call means only an invalidation attempt, not a global deletion guarantee.

Evidence command/output, UTC timestamp, owner, and result:

```text
Not run — RabbitMQ and Redis runtime evidence are not available.
```

## Rollback and release decision

Do not deploy while any required release input or TEST-019–022 evidence is missing. Roll back an application change by disabling the new CDC route/listeners and retaining the slot, persistent offset, raw/DLQ messages, and advertisement schema. Any source discontinuity requires the approved new-epoch/rebuild path; it is not repaired by skipping messages or changing the old managed migration.
