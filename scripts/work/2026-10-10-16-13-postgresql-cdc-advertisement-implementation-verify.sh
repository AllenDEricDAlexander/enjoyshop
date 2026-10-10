#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git -C "$(dirname "$0")/../.." rev-parse --show-toplevel)"
mode="${1:-}"
case "$mode" in
  --self-test|--topology|--schema) ;;
  --standards|--deployment|--final)
    printf 'CHECK_PENDING: %s is added by its owning Plan Step\n' "$mode" >&2
    exit 77
    ;;
  *)
    printf 'usage: %s --self-test|--topology|--schema\n' "$0" >&2
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
