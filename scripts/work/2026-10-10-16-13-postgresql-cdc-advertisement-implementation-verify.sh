#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git -C "$(dirname "$0")/../.." rev-parse --show-toplevel)"
mode="${1:-}"
case "$mode" in
  --self-test|--topology|--schema|--deployment) ;;
  --standards|--final)
    printf 'CHECK_PENDING: %s is added by its owning Plan Step\n' "$mode" >&2
    exit 77
    ;;
  *)
    printf 'usage: %s --self-test|--topology|--schema|--deployment\n' "$0" >&2
    exit 2
    ;;
esac

python3 - "$repo_root" "$mode" <<'PY'
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys

root = Path(sys.argv[1])
mode = sys.argv[2]
logical_groups = ("master_data", "shard_0", "shard_1")
single_tables = {
    "spu", "sku", "users", "roles", "permissions", "user_roles", "role_permissions",
    "courses", "brand", "category", "template", "spec", "para", "category_brand", "ad",
}
shard_tables = {
    "school_classes": {"school_classes_0", "school_classes_1"},
    "class_course_schedules": {"class_course_schedules_0", "class_course_schedules_1"},
}
physical_role_pairs = {
    "master_data": ("master_data_primary", "master_data_replica_0"),
    "shard_0": ("shard_0_primary", "shard_0_replica_0"),
    "shard_1": ("shard_1_primary", "shard_1_replica_0"),
}
module = Path("enjoyshop-service/enjoyshop-service-goods")
resource_root = module / "src/main/resources"
migration_relative = Path("db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql")
manifest_relative = Path("db/egon-mp/repository-manifest.json")
view_relative = module / "codegen/ad-create.sql"
expected_ad_columns = (
    "id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time",
    "deleted_at", "version", "name", "position", "image_url", "target_url", "status",
    "start_time", "end_time", "sort_order",
)
historical_scripts = (
    ("20260913_001", "db/egon-mp/V20260913_001__initialize_repository_schema.sql",
     "e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31"),
    ("20260923_001", "db/egon-mp/V20260923_001__initialize_goods_schema.sql",
     "1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17"),
    ("20260929_001", "db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql",
     "6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83"),
    ("20261009_001", "db/egon-mp/V20261009_001__initialize_product_schema.sql",
     "db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c"),
)


def problem(message):
    raise AssertionError(message)


def check_layout(native_text, profile_texts, old_prefix, actual_prefix):
    for group, (primary, replica) in physical_role_pairs.items():
        if f"      {group}:" not in native_text:
            problem(f"missing read/write group {group}")
        if f"writeDataSourceName: {primary}" not in native_text:
            problem(f"wrong primary source for {group}")
        if f"readDataSourceNames: [{replica}]" not in native_text:
            problem(f"missing replica source for {group}")
        if native_text.count("transactionalReadQueryStrategy: PRIMARY") < 3:
            problem("all groups must keep transactions on PRIMARY")
    single = native_text.split("  - !SINGLE", 1)
    if len(single) != 2:
        problem("missing native SINGLE rule")
    single_body = single[1].split("  - !SHARDING", 1)[0]
    declared_single = set(re.findall(r"master_data\.public\.([a-z_]+)", single_body))
    if declared_single != single_tables:
        problem(f"SINGLE table mismatch: missing={single_tables - declared_single}, extra={declared_single - single_tables}")
    sharding = native_text.split("  - !SHARDING", 1)
    if len(sharding) != 2:
        problem("missing native SHARDING rule")
    for logical, targets in shard_tables.items():
        for suffix in targets:
            if f"shard_0.public.{suffix}" not in sharding[1] and f"shard_1.public.{suffix}" not in sharding[1]:
                problem(f"missing SQL-backed shard target {suffix}")
    if "_t0" in native_text or "_t1" in native_text or "_t2" in native_text:
        problem("legacy STRATEGY _t* targets must not be copied into the approved NATIVE layout")
    for label, text in profile_texts.items():
        expected = (
            "config-style: NATIVE",
            "native-rules-resource: classpath:egon-ss-readwrite.yml",
            "transaction-default-type: LOCAL",
        )
        for value in expected:
            if value not in text:
                problem(f"{label}: missing or wrong profile setting {value}")
        mode_values = re.findall(r"(?m)^[ \t]*mode:\s*(\S+)\s*$", text)
        if len(mode_values) != 1 or mode_values[0] not in (
                "SHARDING_READWRITE", "${APP_DATASOURCE_MODE:SHARDING_READWRITE}"):
            problem(f"{label}: wrong datasource mode {mode_values}")
        for group, (primary, replica) in physical_role_pairs.items():
            for name, role in ((primary, "PRIMARY"), (replica, "REPLICA")):
                binding = rf"name: {re.escape(name)}\s+logical-name: {re.escape(group)}\s+role: {role}\b"
                if not re.search(binding, text):
                    problem(f"{label}: missing physical pool {name} / {group} / role {role}")
    if not old_prefix.startswith("unchanged:") or not actual_prefix.startswith("unchanged:"):
        problem("immutable managed-DDL prefix fixture changed")


def matching_parenthesis(sql, opening):
    depth = 0
    in_string = False
    index = opening
    while index < len(sql):
        character = sql[index]
        if character == "'":
            if in_string and index + 1 < len(sql) and sql[index + 1] == "'":
                index += 2
                continue
            in_string = not in_string
        elif not in_string:
            if character == "(":
                depth += 1
            elif character == ")":
                depth -= 1
                if depth == 0:
                    return index + 1
        index += 1
    problem("CREATE TABLE ad has unbalanced parentheses")


def create_only_view(migration):
    table_matches = list(re.finditer(r"(?im)^\s*CREATE\s+TABLE\s+(?:public\.)?ad\s*\(", migration))
    if len(table_matches) != 1:
        problem(f"expected exactly one CREATE TABLE ad, found {len(table_matches)}")
    match = table_matches[0]
    table_end = matching_parenthesis(migration, match.end() - 1)
    table = migration[match.start():table_end].strip() + ";"
    columns = tuple(re.findall(
        r"(?im)^\s{12}([a-z][a-z0-9_]*)\s+(?:BIGINT|VARCHAR|TIMESTAMP|CHAR|INTEGER)\b", table))
    if columns != expected_ad_columns:
        problem(f"ad column set mismatch: {columns}")
    indexes = [item.group(0).strip() for item in re.finditer(
        r"(?im)^\s*CREATE\s+INDEX\s+idx_ad_[a-z0-9_]+\s+ON\s+ad\s*\(.*?;", migration, re.DOTALL)]
    if len(indexes) != 2:
        problem(f"expected exactly two ad indexes, found {len(indexes)}")
    all_indexes = re.findall(r"(?im)^\s*CREATE\s+(?:UNIQUE\s+)?INDEX\b", migration)
    if len(all_indexes) != 2 or re.search(r"(?im)^\s*CREATE\s+UNIQUE\s+INDEX\b", migration):
        problem("migration must contain only the two approved non-unique ad indexes")
    if re.search(r"(?im)^\s*(?:ALTER|DROP|COMMENT)\b", table + "\n" + "\n".join(indexes)):
        problem("CREATE-only input contains structural DDL")
    return table, indexes, columns, table + "\n\n" + "\n".join(indexes) + "\n"


def check_schema():
    migration_path = resource_root / migration_relative
    manifest_path = resource_root / manifest_relative
    view_path = root / view_relative
    for path in (migration_path, manifest_path, view_path):
        if not path.is_file():
            problem(f"missing schema input {path.relative_to(root)}")

    migration_bytes = migration_path.read_bytes()
    migration = migration_bytes.decode("utf-8")
    migration_sha = hashlib.sha256(migration_bytes).hexdigest()
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    scripts = manifest.get("scripts", [])
    if len(scripts) != len(historical_scripts) + 1:
        problem(f"manifest must contain the four immutable scripts plus one new script, found {len(scripts)}")
    for index, (version, path, expected_sha) in enumerate(historical_scripts):
        entry = scripts[index]
        if (entry.get("version"), entry.get("path"), entry.get("sha256")) != (version, path, expected_sha):
            problem(f"immutable manifest prefix changed at {version}")
        historical_path = resource_root / path
        if not historical_path.is_file() or hashlib.sha256(historical_path.read_bytes()).hexdigest() != expected_sha:
            problem(f"immutable SQL bytes changed at {path}")
        result = subprocess.run(["git", "diff", "--quiet", "HEAD", "--", str((module / "src/main/resources" / path).as_posix())], cwd=root)
        if result.returncode != 0:
            problem(f"tracked historical SQL changed: {path}")

    latest = scripts[-1]
    if (latest.get("version"), latest.get("path"), latest.get("sha256")) != (
            "20261010_001", migration_relative.as_posix(), migration_sha):
        problem("new migration manifest path, version, or SHA-256 is incorrect")

    table, indexes, columns, expected_view = create_only_view(migration)
    if "IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN" not in migration:
        problem("migration lacks the MASTER_DATA role guard")
    if "ELSIF current_setting('egon_migration.role') = 'SHARD' THEN" not in migration:
        problem("migration lacks the explicit SHARD branch")
    if "RAISE EXCEPTION 'Unknown managed DDL role';" not in migration:
        problem("migration must reject an unknown role")
    if "REFERENCES" in table or "UNIQUE" in table:
        problem("ad must not add a foreign key or business uniqueness constraint")
    if "CONSTRAINT ck_ad_status CHECK (status IN ('0', '1'))" not in table:
        problem("ad status enum constraint is missing or changed")
    if migration.count("REPLICA IDENTITY FULL") != 3:
        problem("ad, spu, and sku must each set REPLICA IDENTITY FULL in this version")
    index_names = {re.search(r"CREATE\s+INDEX\s+([a-z0-9_]+)", item, re.IGNORECASE).group(1) for item in indexes}
    if index_names != {"idx_ad_active_slot", "idx_ad_admin_tenant_id"}:
        problem(f"ad index inventory changed: {sorted(index_names)}")
    if view_path.read_text(encoding="utf-8") != expected_view:
        problem("CREATE-only view differs from the new migration's table/index definitions")
    view_sha = hashlib.sha256(view_path.read_bytes()).hexdigest()
    print(f"PASS: V20261010_001 SHA-256, immutable four-script prefix, {len(columns)} ad columns, two indexes, three Replica Identity changes, CREATE-only view {view_sha}")


def read_properties(path):
    properties = {}
    for line_number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            problem(f"invalid property line {path.relative_to(root)}:{line_number}")
        key, value = line.split("=", 1)
        if key in properties:
            problem(f"duplicate property {key} in {path.relative_to(root)}")
        properties[key.strip()] = value.strip()
    return properties


def validate_debezium_properties(properties):
    exact = {
        "debezium.source.connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "debezium.source.offset.storage": "org.apache.kafka.connect.storage.FileOffsetBackingStore",
        "debezium.source.plugin.name": "pgoutput",
        "debezium.source.publication.autocreate.mode": "disabled",
        "debezium.source.schema.include.list": "public",
        "debezium.source.table.include.list": "public.ad,public.spu,public.sku",
        "debezium.source.snapshot.mode": "initial",
        "debezium.source.provide.transaction.metadata": "true",
        "debezium.source.skip.messages.without.change": "false",
        "debezium.source.skipped.operations": "none",
        "debezium.source.tombstones.on.delete": "false",
        "debezium.source.heartbeat.interval.ms": "0",
        "debezium.format.key": "json",
        "debezium.format.key.schemas.enable": "false",
        "debezium.format.value": "json",
        "debezium.format.value.schemas.enable": "false",
        "debezium.sink.type": "rabbitmq",
        "debezium.sink.rabbitmq.exchange": "enjoyshop.cdc.raw.v1",
        "debezium.sink.rabbitmq.routingKey": "master-data",
        "debezium.sink.rabbitmq.routingKey.source": "static",
        "debezium.sink.rabbitmq.routingKeyDurable": "true",
        "debezium.sink.rabbitmq.autoCreateRoutingKey": "false",
        "debezium.sink.rabbitmq.deliveryMode": "2",
    }
    for key, value in exact.items():
        if properties.get(key) != value:
            problem(f"Debezium property {key} must be {value!r}")
    placeholders = {
        "debezium.source.offset.storage.file.filename": "${CDC_OFFSET_FILE:/data/debezium/offsets/offsets.dat}",
        "debezium.source.topic.prefix": "${CDC_SOURCE_NAME}",
        "debezium.source.database.hostname": "${CDC_PG_PRIMARY_HOST}",
        "debezium.source.database.port": "${CDC_PG_PRIMARY_PORT:5432}",
        "debezium.source.database.user": "${CDC_PG_REPLICATION_USER}",
        "debezium.source.database.password": "${CDC_PG_REPLICATION_PASSWORD}",
        "debezium.source.database.dbname": "${CDC_DATABASE}",
        "debezium.source.database.sslmode": "${CDC_PG_SSLMODE:verify-full}",
        "debezium.source.database.sslrootcert": "${CDC_PG_SSLROOTCERT}",
        "debezium.source.slot.name": "${CDC_SLOT_NAME}",
        "debezium.source.publication.name": "${CDC_PUBLICATION_NAME}",
        "debezium.sink.rabbitmq.connection.host": "${CDC_RABBIT_HOST}",
        "debezium.sink.rabbitmq.connection.port": "${CDC_RABBIT_PORT:5672}",
        "debezium.sink.rabbitmq.connection.username": "${CDC_RABBIT_USER}",
        "debezium.sink.rabbitmq.connection.password": "${CDC_RABBIT_PASSWORD}",
        "debezium.sink.rabbitmq.ackTimeout": "${CDC_RABBIT_CONFIRM_TIMEOUT_MS:30000}",
    }
    for key, value in placeholders.items():
        if properties.get(key) != value:
            problem(f"Debezium property {key} must remain a release input placeholder")
    offset_file = properties.get("debezium.source.offset.storage.file.filename", "")
    if "emptyDir" in offset_file or "MemoryOffsetBackingStore" in offset_file:
        problem("Debezium offsets must use the documented persistent file path")
    if any(key.startswith("debezium.transforms") for key in properties):
        problem("raw Debezium before/after payload must not be removed by SMT configuration")
    for key, value in properties.items():
        if "password" in key.lower() and not re.fullmatch(r"\$\{CDC_[A-Z0-9_]+\}", value):
            problem(f"Debezium credential {key} must remain an environment reference")


def check_release_reference(reference):
    return bool(re.fullmatch(r"[^\s@]+@sha256:[a-f0-9]{64}", reference)) and "latest" not in reference.lower()


def check_deployment_documents():
    paths = {
        "properties": root / "deployment/cdc/debezium/application.properties",
        "postgres": root / "deployment/cdc/postgres/replication-contract.md",
        "rabbit": root / "deployment/cdc/rabbit/topology-contract.md",
        "acceptance": root / "deployment/cdc/acceptance.md",
        "readme": root / "deployment/cdc/README.md",
        "verify": root / "scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh",
    }
    for path in paths.values():
        if not path.is_file():
            problem(f"missing CDC deployment contract: {path.relative_to(root)}")
    properties = read_properties(paths["properties"])
    validate_debezium_properties(properties)
    postgres = paths["postgres"].read_text(encoding="utf-8")
    rabbit = paths["rabbit"].read_text(encoding="utf-8")
    acceptance = paths["acceptance"].read_text(encoding="utf-8")
    readme = paths["readme"].read_text(encoding="utf-8")
    verify = paths["verify"].read_text(encoding="utf-8")
    for token in ("pg_replication_slots", "restart_lsn", "confirmed_flush_lsn", "RPO", "fence",
                  "publication.autocreate.mode=disabled"):
        if token not in postgres:
            problem(f"PostgreSQL contract is missing {token}")
    for token in ("enjoyshop.cdc.raw.v1", "enjoyshop.sync.v1", "enjoyshop.cdc.dead.v1",
                  "enjoyshop.cdc.unrouted.v1", "at-least-once", "reject-publish", "no consumer"):
        if token not in rabbit:
            problem(f"Rabbit topology contract is missing {token}")
    for test_id in ("TEST-019", "TEST-020", "TEST-021", "TEST-022"):
        if test_id not in acceptance:
            problem(f"acceptance record is missing {test_id}")
    if "deployment release is blocked" not in acceptance or "Not run" not in acceptance:
        problem("acceptance record must keep live release blocked and manual tests Not run")
    for input_name in ("CDC_SOURCE_NAME", "CDC_SOURCE_EPOCH", "CDC_RABBIT_VHOST", "sha256"):
        if input_name not in readme and input_name not in acceptance:
            problem(f"README/acceptance must declare {input_name} as a release input")
    if "one active" not in readme or "persistent offset" not in readme or "NetworkPolicy" not in readme:
        problem("README is missing the single-owner K8s, persistent offset, or network boundary")
    if "--deployment" not in verify or "--self-test" not in verify:
        problem("offline verifier does not expose the documented deployment modes")
    for profile in ("application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml"):
        text = (root / module / "src/main/resources" / profile).read_text(encoding="utf-8")
        cdc_pattern = r"(?m)^  cdc:\n    enabled: false\n    max-payload-bytes: 1048576\n    confirm-timeout: PT5S\n    source-registry: \{\}$"
        rabbit_pattern = r"(?m)^  rabbitmq:\n    publisher-confirm-type: correlated\n    publisher-returns: true$"
        if not re.search(cdc_pattern, text):
            problem(f"{profile} must keep the CDC source registry explicit and disabled by default")
        if not re.search(rabbit_pattern, text):
            problem(f"{profile} lacks the Rabbit confirm/return settings")
    for relative, expected_hash in (
            ("src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java",
             "5d8f721d5a5a79718034e5be009c1ea615d2a8de7dbef47da8934e1dfada5ab5"),
            ("src/main/java/top/egon/enjoyshop/goods/infrastructure/mq/MqRouteEnum.java",
             "bdbb81af71feb66808b439666cafdf445ea7d9714c5c219f3947746277e99258")):
        actual = hashlib.sha256((root / module / relative).read_bytes()).hexdigest()
        if actual != expected_hash:
            problem(f"original sample Rabbit file changed: {relative}")
    pom = (root / module / "pom.xml").read_text(encoding="utf-8").lower()
    if any(name in pom for name in ("debezium", "kafka-clients", "elasticsearch", "transactional-outbox-starter")):
        problem("goods POM must not acquire Debezium, Kafka, search, or Outbox dependencies")
    return properties


def deployment_self_test():
    properties = read_properties(root / "deployment/cdc/debezium/application.properties")
    validate_debezium_properties(properties)
    mutations = (
        ("debezium.source.table.include.list", "public.ad,public.spu"),
        ("debezium.source.publication.autocreate.mode", "all_tables"),
        ("debezium.source.skipped.operations", "t"),
        ("debezium.source.skip.messages.without.change", "true"),
        ("debezium.source.tombstones.on.delete", "true"),
        ("debezium.format.value.schemas.enable", "true"),
        ("debezium.sink.rabbitmq.autoCreateRoutingKey", "true"),
        ("debezium.sink.rabbitmq.deliveryMode", "1"),
        ("debezium.source.offset.storage.file.filename", "emptyDir/offsets.dat"),
    )
    for key, bad_value in mutations:
        mutated = dict(properties)
        mutated[key] = bad_value
        try:
            validate_debezium_properties(mutated)
        except AssertionError:
            continue
        problem(f"deployment self-test accepted an invalid property: {key}={bad_value}")
    if check_release_reference("debezium/server:latest"):
        problem("deployment self-test accepted an unpinned latest image")
    if not check_release_reference("registry.example/debezium/server@sha256:" + "a" * 64):
        problem("deployment self-test rejected a pinned image fixture")
    check_deployment_documents()
    print("PASS: CDC profile defaults, source filters, fixed raw route, persistent offsets, and release-input mutations")


def check_deployment():
    check_deployment_documents()
    print("PASS: CDC source/sink configuration and PostgreSQL/Rabbit/Kubernetes offline contracts")
    print("RELEASE BLOCKED (expected): version/image digest and TEST-019–022 runtime evidence remain unprovided/Not run")


def read_properties(path):
    properties = {}
    for line_number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            problem(f"invalid property line {path.relative_to(root)}:{line_number}")
        key, value = line.split("=", 1)
        if key in properties:
            problem(f"duplicate property {key} in {path.relative_to(root)}")
        properties[key.strip()] = value.strip()
    return properties


def validate_debezium_properties(properties):
    exact = {
        "debezium.source.connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "debezium.source.offset.storage": "org.apache.kafka.connect.storage.FileOffsetBackingStore",
        "debezium.source.plugin.name": "pgoutput",
        "debezium.source.publication.autocreate.mode": "disabled",
        "debezium.source.schema.include.list": "public",
        "debezium.source.table.include.list": "public.ad,public.spu,public.sku",
        "debezium.source.snapshot.mode": "initial",
        "debezium.source.provide.transaction.metadata": "true",
        "debezium.source.skip.messages.without.change": "false",
        "debezium.source.skipped.operations": "none",
        "debezium.source.tombstones.on.delete": "false",
        "debezium.source.heartbeat.interval.ms": "0",
        "debezium.format.key": "json",
        "debezium.format.key.schemas.enable": "false",
        "debezium.format.value": "json",
        "debezium.format.value.schemas.enable": "false",
        "debezium.sink.type": "rabbitmq",
        "debezium.sink.rabbitmq.exchange": "enjoyshop.cdc.raw.v1",
        "debezium.sink.rabbitmq.routingKey": "master-data",
        "debezium.sink.rabbitmq.routingKey.source": "static",
        "debezium.sink.rabbitmq.routingKeyDurable": "true",
        "debezium.sink.rabbitmq.autoCreateRoutingKey": "false",
        "debezium.sink.rabbitmq.deliveryMode": "2",
    }
    for key, value in exact.items():
        if properties.get(key) != value:
            problem(f"Debezium property {key} must be {value!r}")
    placeholders = {
        "debezium.source.offset.storage.file.filename": "${CDC_OFFSET_FILE:/data/debezium/offsets/offsets.dat}",
        "debezium.source.topic.prefix": "${CDC_SOURCE_NAME}",
        "debezium.source.database.hostname": "${CDC_PG_PRIMARY_HOST}",
        "debezium.source.database.port": "${CDC_PG_PRIMARY_PORT:5432}",
        "debezium.source.database.user": "${CDC_PG_REPLICATION_USER}",
        "debezium.source.database.password": "${CDC_PG_REPLICATION_PASSWORD}",
        "debezium.source.database.dbname": "${CDC_DATABASE}",
        "debezium.source.database.sslmode": "${CDC_PG_SSLMODE:verify-full}",
        "debezium.source.database.sslrootcert": "${CDC_PG_SSLROOTCERT}",
        "debezium.source.slot.name": "${CDC_SLOT_NAME}",
        "debezium.source.publication.name": "${CDC_PUBLICATION_NAME}",
        "debezium.sink.rabbitmq.connection.host": "${CDC_RABBIT_HOST}",
        "debezium.sink.rabbitmq.connection.port": "${CDC_RABBIT_PORT:5672}",
        "debezium.sink.rabbitmq.connection.username": "${CDC_RABBIT_USER}",
        "debezium.sink.rabbitmq.connection.password": "${CDC_RABBIT_PASSWORD}",
        "debezium.sink.rabbitmq.ackTimeout": "${CDC_RABBIT_CONFIRM_TIMEOUT_MS:30000}",
    }
    for key, value in placeholders.items():
        if properties.get(key) != value:
            problem(f"Debezium property {key} must remain a release input placeholder")
    offset_file = properties.get("debezium.source.offset.storage.file.filename", "")
    if "emptyDir" in offset_file or "MemoryOffsetBackingStore" in offset_file:
        problem("Debezium offsets must use the documented persistent file path")
    if any(key.startswith("debezium.transforms") for key in properties):
        problem("raw Debezium before/after payload must not be removed by SMT configuration")
    if any(value.lower() in ("password", "secret", "changeme", "admin")
           for key, value in properties.items() if "password" in key.lower()):
        problem("Debezium credentials must remain environment inputs")


def check_release_reference(value):
    return bool(re.fullmatch(r"[^\s@]+@sha256:[a-f0-9]{64}", value)) and "latest" not in value.lower()


def check_deployment_documents():
    paths = {
        "properties": root / "deployment/cdc/debezium/application.properties",
        "postgres": root / "deployment/cdc/postgres/replication-contract.md",
        "rabbit": root / "deployment/cdc/rabbit/topology-contract.md",
        "acceptance": root / "deployment/cdc/acceptance.md",
        "readme": root / "deployment/cdc/README.md",
        "verify": root / "scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh",
    }
    for label, path in paths.items():
        if not path.is_file():
            problem(f"missing CDC deployment contract: {path.relative_to(root)}")
    properties = read_properties(paths["properties"])
    validate_debezium_properties(properties)
    postgres = paths["postgres"].read_text(encoding="utf-8")
    rabbit = paths["rabbit"].read_text(encoding="utf-8")
    acceptance = paths["acceptance"].read_text(encoding="utf-8")
    readme = paths["readme"].read_text(encoding="utf-8")
    verify = paths["verify"].read_text(encoding="utf-8")
    for token in ("pg_replication_slots", "restart_lsn", "confirmed_flush_lsn", "RPO", "fence",
                  "publication.autocreate.mode=disabled"):
        if token not in postgres:
            problem(f"PostgreSQL contract is missing {token}")
    for token in ("enjoyshop.cdc.raw.v1", "enjoyshop.sync.v1", "enjoyshop.cdc.dead.v1",
                  "enjoyshop.cdc.unrouted.v1", "at-least-once", "reject-publish", "no consumer"):
        if token not in rabbit:
            problem(f"Rabbit topology contract is missing {token}")
    for test_id in ("TEST-019", "TEST-020", "TEST-021", "TEST-022"):
        if test_id not in acceptance:
            problem(f"acceptance record is missing {test_id}")
    if "deployment release is blocked" not in acceptance or "Not run" not in acceptance:
        problem("acceptance record must keep live release blocked and manual tests Not run")
    for input_name in ("CDC_SOURCE_NAME", "CDC_SOURCE_EPOCH", "CDC_RABBIT_VHOST", "sha256"):
        if input_name not in readme and input_name not in acceptance:
            problem(f"README/acceptance must declare {input_name} as a release input")
    if "one active" not in readme or "persistent offset" not in readme or "NetworkPolicy" not in readme:
        problem("README is missing the single-owner K8s, persistent offset, or network boundary")
    if "--deployment" not in verify or "--self-test" not in verify:
        problem("offline verifier does not expose the documented deployment modes")
    for profile in ("application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml"):
        text = (root / module / "src/main/resources" / profile).read_text(encoding="utf-8")
        if not re.search(r"(?m)^  cdc:\n    enabled: false\n    max-payload-bytes: 1048576\n    confirm-timeout: PT5S\n    source-registry: \{\}$", text):
            problem(f"{profile} must keep the CDC source registry explicit and disabled by default")
        if not re.search(r"(?m)^  rabbitmq:\n    publisher-confirm-type: correlated\n    publisher-returns: true$", text):
            problem(f"{profile} lacks the Rabbit confirm/return settings")
    for relative, expected_hash in (
            ("src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java",
             "5d8f721d5a5a79718034e5be009c1ea615d2a8de7dbef47da8934e1dfada5ab5"),
            ("src/main/java/top/egon/enjoyshop/goods/infrastructure/mq/MqRouteEnum.java",
             "bdbb81af71feb66808b439666cafdf445ea7d9714c5c219f3947746277e99258")):
        actual = hashlib.sha256((root / module / relative).read_bytes()).hexdigest()
        if actual != expected_hash:
            problem(f"original sample Rabbit file changed: {relative}")
    pom = (root / module / "pom.xml").read_text(encoding="utf-8").lower()
    if any(name in pom for name in ("debezium", "kafka-clients", "elasticsearch", "transactional-outbox-starter")):
        problem("goods POM must not acquire Debezium, Kafka, search, or Outbox dependencies")
    return properties, acceptance


def deployment_self_test():
    properties_path = root / "deployment/cdc/debezium/application.properties"
    properties = read_properties(properties_path)
    validate_debezium_properties(properties)
    mutations = (
        ("debezium.source.table.include.list", "public.ad,public.spu"),
        ("debezium.source.publication.autocreate.mode", "all_tables"),
        ("debezium.source.skipped.operations", "t"),
        ("debezium.source.skip.messages.without.change", "true"),
        ("debezium.source.tombstones.on.delete", "true"),
        ("debezium.format.value.schemas.enable", "true"),
        ("debezium.sink.rabbitmq.autoCreateRoutingKey", "true"),
        ("debezium.sink.rabbitmq.deliveryMode", "1"),
        ("debezium.source.offset.storage.file.filename", "emptyDir/offsets.dat"),
    )
    for key, bad_value in mutations:
        mutated = dict(properties)
        mutated[key] = bad_value
        try:
            validate_debezium_properties(mutated)
        except AssertionError:
            continue
        problem(f"deployment self-test accepted an invalid property: {key}={bad_value}")
    if check_release_reference("debezium/server@latest"):
        problem("deployment self-test accepted an unpinned latest image")
    if not check_release_reference("registry.example/debezium/server@sha256:" + "a" * 64):
        problem("deployment self-test rejected a syntactically pinned image fixture")
    check_deployment_documents()
    print("PASS: disabled CDC defaults, preserved row images, fixed routes, persistent offsets, and release-gate mutation tests")


def check_deployment():
    _, acceptance = check_deployment_documents()
    print("PASS: CDC server, PostgreSQL, Rabbit topology, profile, and offline acceptance contracts")
    print("RELEASE BLOCKED (expected): version/image digest, environment inputs, and TEST-019–022 remain unprovided/Not run")


if mode == "--self-test":
    native = """  - !READWRITE_SPLITTING\n    dataSourceGroups:\n      master_data:\n        writeDataSourceName: master_data_primary\n        readDataSourceNames: [master_data_replica_0]\n        transactionalReadQueryStrategy: PRIMARY\n      shard_0:\n        writeDataSourceName: shard_0_primary\n        readDataSourceNames: [shard_0_replica_0]\n        transactionalReadQueryStrategy: PRIMARY\n      shard_1:\n        writeDataSourceName: shard_1_primary\n        readDataSourceNames: [shard_1_replica_0]\n        transactionalReadQueryStrategy: PRIMARY\n  - !SINGLE\n""" + "\n".join(f"      - master_data.public.{table}" for table in sorted(single_tables)) + "\n  - !SHARDING\n" + "\n".join(
        f"      actualDataNodes: shard_0.public.{table}_0,shard_0.public.{table}_1,shard_1.public.{table}_0,shard_1.public.{table}_1"
        for table in sorted(shard_tables)
    )
    profiles = {f"application-{name}": "\n".join((
        "config-style: NATIVE",
        "native-rules-resource: classpath:egon-ss-readwrite.yml",
        "mode: ${APP_DATASOURCE_MODE:SHARDING_READWRITE}",
        "transaction-default-type: LOCAL",
        *[f"name: {pool}\nlogical-name: {group}\nrole: {role}" for group, values in physical_role_pairs.items() for pool, role in ((values[0], "PRIMARY"), (values[1], "REPLICA"))],
    )) for name in ("base", "dev", "test", "prod")}
    profiles["egon-mybatis-plus-sharding.yml"] = profiles["application-base"]
    check_layout(native, profiles, "unchanged:old-manifest-prefix", "unchanged:old-manifest-prefix")
    mutations = (
        (native.replace("readDataSourceNames: [shard_1_replica_0]", "readDataSourceNames: []", 1), profiles, "unchanged:old", "unchanged:old"),
        (native, {**profiles, "application-test": profiles["application-test"].replace("role: REPLICA", "role: PRIMARY", 1)}, "unchanged:old", "unchanged:old"),
        (native, profiles, "changed:old-manifest-prefix", "changed:old-manifest-prefix"),
    )
    for bad in mutations:
        try:
            check_layout(*bad)
        except AssertionError:
            continue
        problem("self-test accepted an invalid read/write/topology/history fixture")
    print("PASS: valid NATIVE/profile fixture accepted; missing replica, wrong role, and changed history rejected")
    deployment_self_test()
elif mode == "--deployment":
    check_deployment()
elif mode == "--topology":
    native_path = root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml"
    profile_paths = {
        "application.yml": root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml",
        "application-dev.yml": root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml",
        "application-test.yml": root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml",
        "application-prod.yml": root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml",
        "egon-mybatis-plus-sharding.yml": root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml",
    }
    for path in [native_path, *profile_paths.values()]:
        if not path.is_file():
            problem(f"missing NATIVE configuration path {path.relative_to(root)}")
    profile_texts = {name: path.read_text(encoding="utf-8") for name, path in profile_paths.items()}
    old_manifest_prefix = root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json"
    old_ddl = root / "enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp"
    if not old_manifest_prefix.is_file() or not old_ddl.is_dir():
        problem("managed SQL/manifest baseline missing")
    for old_path in sorted(old_ddl.glob("V*.sql")):
        result = subprocess.run(["git", "diff", "--quiet", "HEAD", "--", str(old_path.relative_to(root))], cwd=root)
        if result.returncode != 0:
            problem(f"immutable database history changed: {old_path.relative_to(root)}")
    check_layout(native_path.read_text(encoding="utf-8"), profile_texts, "unchanged:HEAD", "unchanged:HEAD")
    generated_test = root / "enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java"
    if not generated_test.is_file() or "nativeProfilesAreParseableWithoutConnections" not in generated_test.read_text(encoding="utf-8"):
        problem("NativeReadwriteContractTest is absent or incomplete")
    print("PASS: native groups, SQL-backed tables, six physical roles, five profile resources, and immutable DDL prefix")
else:
    check_schema()
PY
