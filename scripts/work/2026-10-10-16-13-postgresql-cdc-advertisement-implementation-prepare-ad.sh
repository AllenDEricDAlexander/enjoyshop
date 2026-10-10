#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git -C "$(dirname "$0")/../.." rev-parse --show-toplevel)"
mode="${1:-}"
stage="${2:-}"
case "$mode" in
  --extract-create|--verify-generated-scope) ;;
  --generator-stage)
    case "$stage" in
      persistence|application|controller) ;;
      *)
        printf 'usage: %s --generator-stage persistence|application|controller\n' "$0" >&2
        exit 2
        ;;
    esac
    ;;
  *)
    printf 'usage: %s --extract-create|--generator-stage persistence|application|controller|--verify-generated-scope\n' "$0" >&2
    exit 2
    ;;
esac

python3 - "$repo_root" "$mode" "$stage" <<'PY'
from hashlib import sha256
import json
from pathlib import Path
import re
import subprocess
import sys

root = Path(sys.argv[1])
mode = sys.argv[2]
stage = sys.argv[3]
module = Path("enjoyshop-service/enjoyshop-service-goods")
resource_root = root / module / "src/main/resources"
migration_relative = Path("db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql")
manifest_relative = Path("db/egon-mp/repository-manifest.json")
config_relative = module / "codegen/ad-codegen.json"
view_relative = module / "codegen/ad-create.sql"
expected_columns = (
    "id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time",
    "deleted_at", "version", "name", "position", "image_url", "target_url", "status",
    "start_time", "end_time", "sort_order",
)
stage_artifacts = {
    "persistence": ["po", "dao", "mapper-xml", "repo", "domain-model", "domain-query"],
    "application": ["command", "query", "result", "converter", "domain-service", "domain-impl", "manage", "manage-impl"],
    "controller": ["controller"],
}


def require(condition, message):
    if not condition:
        raise SystemExit(f"BLOCKED_SCOPE: {message}")


def migration_bytes():
    path = resource_root / migration_relative
    require(path.is_file(), f"missing migration {path.relative_to(root)}")
    return path.read_bytes()


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
    raise SystemExit("BLOCKED_SCOPE: CREATE TABLE ad has unbalanced parentheses")


def extract_create():
    source = migration_bytes()
    source_hash = sha256(source).hexdigest()
    manifest = json.loads((resource_root / manifest_relative).read_text(encoding="utf-8"))
    entries = [entry for entry in manifest["scripts"] if entry.get("version") == "20261010_001"]
    require(len(entries) == 1, "manifest must contain exactly one 20261010_001 entry")
    require(entries[0].get("path") == migration_relative.as_posix(), "manifest path does not match migration")
    require(entries[0].get("sha256") == source_hash, "manifest SHA-256 does not match migration bytes")

    sql = source.decode("utf-8")
    table_matches = list(re.finditer(r"(?im)^\s*CREATE\s+TABLE\s+(?:public\.)?ad\s*\(", sql))
    require(len(table_matches) == 1, "migration must contain exactly one CREATE TABLE ad")
    table_match = table_matches[0]
    table_end = matching_parenthesis(sql, table_match.end() - 1)
    table_sql = sql[table_match.start():table_end].strip() + ";"
    columns = tuple(re.findall(r"(?im)^\s{12}([a-z][a-z0-9_]*)\s+(?:BIGINT|VARCHAR|TIMESTAMP|CHAR|INTEGER)\b", table_sql))
    require(columns == expected_columns, f"CREATE TABLE ad columns differ from approved 16-column contract: {columns}")

    indexes = [match.group(0).strip() for match in re.finditer(
        r"(?im)^\s*CREATE\s+INDEX\s+idx_ad_[a-z0-9_]+\s+ON\s+ad\s*\(.*?;", sql, re.DOTALL)]
    require(len(indexes) == 2, "migration must contain exactly the two approved non-unique ad indexes")
    view = table_sql + "\n\n" + "\n".join(indexes) + "\n"
    require(not re.search(r"(?im)^\s*(?:ALTER|DROP|COMMENT)\b", view), "CREATE-only generator view contains structural DDL")
    require("REPLICA IDENTITY FULL" not in view, "generator view must exclude CDC ALTER statements")

    destination = root / view_relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(view, encoding="utf-8")
    print(json.dumps({
        "status": "EXTRACTED",
        "sourcePath": (resource_root / migration_relative).relative_to(root).as_posix(),
        "sourceSha256": source_hash,
        "viewPath": view_relative.as_posix(),
        "viewSha256": sha256(view.encode("utf-8")).hexdigest(),
        "columns": list(columns),
        "indexCount": len(indexes),
    }, separators=(",", ":")))


def set_generator_stage():
    path = root / config_relative
    require(path.is_file(), f"missing generator config {config_relative.as_posix()}")
    config = json.loads(path.read_text(encoding="utf-8"))
    require(config.get("projectType") == "light", "generator profile must remain native Light")
    require(config.get("domain") == "ad" and config.get("logicalTables") == ["ad"],
            "generator scope must remain the single ad logical table")
    require(config.get("input", {}).get("mode") == "schema"
            and config.get("input", {}).get("schemaFiles") == ["ad-create.sql"],
            "generator input must remain the extracted CREATE-only view")
    config["artifacts"] = stage_artifacts[stage]
    path.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": "STAGE_SELECTED", "stage": stage, "artifacts": config["artifacts"]}, separators=(",", ":")))


def verify_generated_scope():
    config = json.loads((root / config_relative).read_text(encoding="utf-8"))
    require(config.get("projectType") == "light" and config.get("domain") == "ad"
            and config.get("logicalTables") == ["ad"], "generator config escaped its approved profile/table scope")
    require(config.get("outputRoot") == module.as_posix(), "generator output root changed")
    require(config.get("input", {}).get("schemaFiles") == ["ad-create.sql"],
            "generator no longer reads only the CREATE-only ad view")
    require(config.get("artifacts") in stage_artifacts.values(), "generator artifact list is outside approved stages")

    status = subprocess.run(
        ["git", "status", "--porcelain", "--untracked-files=all"], cwd=root, check=True,
        text=True, capture_output=True).stdout
    output_root = module / "src/main/java/top/egon/enjoyshop/goods"
    allowed_prefixes = (
        output_root / "infrastructure/ad",
        output_root / "domain/ad",
        output_root / "application/ad",
        output_root / "adapter/goods",
        module / "src/main/resources/mybatis/mapper/ad",
    )
    generated_paths = []
    for line in status.splitlines():
        changed_path = line[3:].strip()
        path = Path(changed_path)
        if path.is_relative_to(module / "src/main/java/top/egon/enjoyshop/goods") or path.is_relative_to(
                module / "src/main/resources/mybatis/mapper"):
            require(any(path.is_relative_to(prefix) for prefix in allowed_prefixes),
                    f"generated output escaped the ad package: {changed_path}")
            generated_paths.append(changed_path)
    print(json.dumps({"status": "SCOPE_VERIFIED", "generatedPaths": sorted(generated_paths)}, separators=(",", ":")))


if mode == "--extract-create":
    extract_create()
elif mode == "--generator-stage":
    set_generator_stage()
else:
    verify_generated_scope()
PY
