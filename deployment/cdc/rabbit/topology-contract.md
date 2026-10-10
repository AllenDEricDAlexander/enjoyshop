# RabbitMQ CDC topology and broker policy contract

Create the exchanges, quorum queues, bindings, policies, and least-privilege accounts before enabling CDC. The Java application declares the named topology when `app.cdc.enabled=true`; this file records the external broker checks that code cannot prove. All queue and exchange names below are environment-isolated by the RabbitMQ vhost selected by the platform.

| Exchange | Queue | Binding | Owner / purpose |
| --- | --- | --- | --- |
| `enjoyshop.cdc.raw.v1` | `enjoyshop.cdc.normalize.v1` | `master-data` | Debezium Server → single-active raw bridge |
| `enjoyshop.sync.v1` | `enjoyshop.ad.invalidate.v1` | `ad.invalidate.v1` | goods advertisement invalidation listener |
| `enjoyshop.sync.v1` | `enjoyshop.search.refresh.v1` | `product.refresh.v1` | future search owner; no consumer in this phase |
| `enjoyshop.cdc.dead.v1` | raw/ad/search `.dead.v1` queues | matching dead-letter routing key | permanent poison and exhausted transient deliveries |
| `enjoyshop.cdc.unrouted.v1` | `enjoyshop.cdc.unrouted.dead.v1` | `#` | raw alternate-exchange quarantine |

The raw queue is durable quorum with `x-single-active-consumer=true` and prefetch 32. The advertisement and search queues are durable quorum and receive persistent semantic messages. Search has no listener or projection store in the current delivery. Preserve its backlog until the next phase has a full rebuild and consumption protocol.

The raw exchange declares `enjoyshop.cdc.unrouted.v1` as its alternate exchange. Verify a deliberately unbound routing key reaches the durable unrouted queue. Raw routing uses a fixed `master-data` key; the Server template disables automatic routing-key creation. This matters because the Debezium Rabbit sink documentation describes routing and confirms, but a publisher confirm alone does not prove that a business queue binding exists. Do not rely on a sink-level mandatory option that the selected release does not document.

Bind raw, advertisement, and search queues to `enjoyshop.cdc.dead.v1` with the names in the table. Apply broker-version-supported quorum at-least-once dead-lettering and `overflow=reject-publish` policies. Keep the original payload and source identity, and configure capacity/retention limits that alert operators before exhaustion. Do not use TTL or drop-head to silently discard source facts. If the selected RabbitMQ release cannot provide the required queue and dead-letter semantics, keep CDC disabled.

Grant the Debezium service account write access only to `enjoyshop.cdc.raw.v1`. The goods bridge publishes only to `enjoyshop.sync.v1`; raw consumers cannot publish arbitrary exchanges, and no public application account can publish synchronization events. Ad consumers validate message ID, schema header, route, source/epoch, tenant, and body before attempting cache invalidation. A successful cache SPI call is an invalidation attempt; it does not prove every pod or Redis L2 deleted a key.

Verify the exact RabbitMQ version, quorum availability, confirms, returns, alternate exchange, routing ACLs, queue durability, manual ACK, poison isolation, and disk/capacity alarms with TEST-022. The current task contains no broker runtime evidence and does not declare the backlog safe for production.
