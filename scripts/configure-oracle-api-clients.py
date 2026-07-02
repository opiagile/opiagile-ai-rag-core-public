#!/usr/bin/env python3
import hashlib
import secrets
import subprocess
from pathlib import Path


ENV_PATH = Path("deploy/oracle-free-tier/.env")


def parse_env(lines):
    env = {}
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        env[key.strip()] = value.strip().strip('"').strip("'")
    return env


def set_env(lines, key, value):
    prefix = key + "="
    for index, line in enumerate(lines):
        if line.startswith(prefix):
            lines[index] = f"{key}={value}"
            return
    lines.append(f"{key}={value}")


def sql_quote(value):
    return "'" + value.replace("'", "''") + "'"


def array_literal(values):
    return "ARRAY[" + ", ".join(sql_quote(value) for value in values) + "]::TEXT[]"


def run_psql(sql, env):
    subprocess.run(
        [
            "docker",
            "exec",
            "-i",
            "opiagile-rag-core-postgres",
            "psql",
            "-U",
            env["POSTGRES_USER"],
            "-d",
            env["POSTGRES_DB"],
            "-v",
            "ON_ERROR_STOP=1",
            "-q",
        ],
        input=sql,
        text=True,
        check=True,
        stdout=subprocess.DEVNULL,
    )


def ensure_client(env, lines, var_name, tenant, workspace, name, scopes, rate_limit):
    current = env.get(var_name, "").strip()
    if current.startswith("opg_"):
        print(f"{var_name}: ja configurada")
        return current

    api_key = "opg_" + secrets.token_urlsafe(32).replace("-", "").replace("_", "")[:40]
    key_hash = hashlib.sha256(api_key.encode()).hexdigest()
    key_prefix = api_key[:12]
    sql = f"""
INSERT INTO api_clients (
    tenant_id,
    workspace_id,
    name,
    key_prefix,
    key_hash,
    scopes,
    rate_limit_per_minute
)
SELECT t.id,
       w.id,
       {sql_quote(name)},
       {sql_quote(key_prefix)},
       {sql_quote(key_hash)},
       {array_literal(scopes)},
       {rate_limit}
FROM tenants t
JOIN workspaces w ON w.tenant_id = t.id
WHERE t.slug = {sql_quote(tenant)}
  AND w.slug = {sql_quote(workspace)};
"""
    run_psql(sql, env)
    set_env(lines, var_name, api_key)
    env[var_name] = api_key
    print(f"{var_name}: criada e cadastrada")
    return api_key


def main():
    lines = ENV_PATH.read_text().splitlines()
    env = parse_env(lines)
    demo_scopes = [
        "chat:write",
        "documents:read",
        "documents:upload",
        "conversations:read",
        "observability:read",
        "workspaces:read",
    ]

    ensure_client(
        env,
        lines,
        "OPIAGILE_SITE_RAG_CORE_API_KEY",
        "opiagile",
        "opiagile-rag",
        "Landing Opiagile",
        ["chat:write"],
        60,
    )
    clinica_key = ensure_client(
        env,
        lines,
        "DEMO_RAG_CORE_API_KEY_CLINICA_DEMO",
        "demo",
        "clinica-demo",
        "Demo RAG Clinica",
        demo_scopes,
        60,
    )
    ensure_client(
        env,
        lines,
        "DEMO_RAG_CORE_API_KEY_ATENDIMENTO_DEMO",
        "demo",
        "atendimento-demo",
        "Demo RAG Atendimento",
        demo_scopes,
        60,
    )
    ensure_client(
        env,
        lines,
        "DEMO_RAG_CORE_API_KEY_LOCACAO_DEMO",
        "demo",
        "locacao-demo",
        "Demo RAG Locacao",
        demo_scopes,
        60,
    )

    if not env.get("DEMO_RAG_CORE_API_KEY", "").startswith("opg_"):
        set_env(lines, "DEMO_RAG_CORE_API_KEY", clinica_key)

    set_env(lines, "API_SECURITY_ENABLED", "true")
    set_env(lines, "API_SECURITY_REQUIRE_API_KEY", "true")
    set_env(lines, "API_SECURITY_DEFAULT_RATE_LIMIT_PER_MINUTE", env.get("API_SECURITY_DEFAULT_RATE_LIMIT_PER_MINUTE", "60") or "60")
    ENV_PATH.write_text("\n".join(lines) + "\n")
    print("API_SECURITY_REQUIRE_API_KEY: true")


if __name__ == "__main__":
    main()
