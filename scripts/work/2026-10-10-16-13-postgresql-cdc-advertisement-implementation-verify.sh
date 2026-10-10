#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git -C "$(dirname "$0")/../.." rev-parse --show-toplevel)"
mode="${1:-}"
case "$mode" in
  --self-test|--topology) ;;
  --schema|--standards|--deployment|--final)
    printf 'CHECK_PENDING: %s is added by its owning Plan Step\n' "$mode" >&2
    exit 77
    ;;
  *)
    printf 'usage: %s --self-test|--topology\n' "$0" >&2
    exit 2
    ;;
esac

python3 - "$repo_root" "$mode" <<'PY'
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
mode = sys.argv[2]
logical_groups = ("master_data", "shard_0", "shard_1")
single_tables = {
    "spu", "sku", "users", "roles", "permissions", "user_roles", "role_permissions",
    "courses", "brand", "category", "template", "spec", "para", "category_brand",
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
else:
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
    import subprocess
    for old_path in [old_manifest_prefix, *sorted(old_ddl.glob("V*.sql"))]:
        result = subprocess.run(["git", "diff", "--quiet", "HEAD", "--", str(old_path.relative_to(root))], cwd=root)
        if result.returncode != 0:
            problem(f"immutable database history changed: {old_path.relative_to(root)}")
    check_layout(native_path.read_text(encoding="utf-8"), profile_texts, "unchanged:HEAD", "unchanged:HEAD")
    generated_test = root / "enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java"
    if not generated_test.is_file() or "nativeProfilesAreParseableWithoutConnections" not in generated_test.read_text(encoding="utf-8"):
        problem("NativeReadwriteContractTest is absent or incomplete")
    print("PASS: native groups, SQL-backed tables, six physical roles, five profile resources, and immutable DDL prefix")
PY
