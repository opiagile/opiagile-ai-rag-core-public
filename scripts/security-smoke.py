#!/usr/bin/env python3
"""Smoke test não destrutivo para a superfície pública da Opiagile.

O script não usa API keys reais, não faz brute force e não envia carga agressiva.
Ele valida apenas contratos de segurança esperados em endpoints públicos.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Iterable


PUBLIC_BASE_URL = os.environ.get("PUBLIC_BASE_URL", "https://opiagile.com").rstrip("/")
DEMO_BASE_URL = os.environ.get("DEMO_BASE_URL", "https://demo-rag.opiagile.com").rstrip("/")
TIMEOUT_SECONDS = int(os.environ.get("SECURITY_SMOKE_TIMEOUT_SECONDS", "15"))


@dataclass(frozen=True)
class Check:
    name: str
    method: str
    url: str
    expected_status: int
    expected_content_type: str | None = None
    required_headers: tuple[str, ...] = ()


def request(method: str, url: str, headers: dict[str, str] | None = None, body: bytes | None = None):
    req = urllib.request.Request(
        url,
        data=body,
        method=method,
        headers={
            "User-Agent": "OpiagileSecuritySmoke/1.0",
            "Accept": "application/json,text/html;q=0.9,*/*;q=0.8",
            **(headers or {}),
        },
    )
    try:
        response = urllib.request.urlopen(req, timeout=TIMEOUT_SECONDS)
        return response.status, response.headers, response.read(400).decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        return error.code, error.headers, error.read(400).decode("utf-8", "replace")


def checks() -> Iterable[Check]:
    security_headers = (
        "Strict-Transport-Security",
        "X-Content-Type-Options",
        "X-Frame-Options",
        "Referrer-Policy",
        "Permissions-Policy",
        "Content-Security-Policy",
    )
    yield Check(
        "home_headers",
        "GET",
        f"{PUBLIC_BASE_URL}/",
        200,
        "text/html",
        security_headers,
    )
    yield Check(
        "health_publico",
        "GET",
        f"{PUBLIC_BASE_URL}/actuator/health",
        200,
        "application/json",
        security_headers,
    )
    yield Check(
        "workspaces_sem_chave",
        "GET",
        f"{PUBLIC_BASE_URL}/api/workspaces",
        401,
        "application/json",
        security_headers,
    )
    yield Check(
        "documents_sem_chave",
        "GET",
        f"{PUBLIC_BASE_URL}/api/documents",
        401,
        "application/json",
        security_headers,
    )
    yield Check(
        "admin_nao_publico",
        "GET",
        f"{PUBLIC_BASE_URL}/api/admin/developer-access-requests",
        404,
        None,
        security_headers,
    )
    yield Check(
        "demo_workspaces_sem_chave",
        "GET",
        f"{DEMO_BASE_URL}/api/workspaces",
        401,
        "application/json",
        security_headers,
    )


def run_check(check: Check) -> dict[str, object]:
    status, headers, body = request(check.method, check.url)
    content_type = headers.get("content-type", "")
    missing_headers = [header for header in check.required_headers if not headers.get(header)]
    ok = status == check.expected_status
    if check.expected_content_type:
        ok = ok and check.expected_content_type in content_type
    ok = ok and not missing_headers
    return {
        "name": check.name,
        "method": check.method,
        "url": check.url,
        "status": status,
        "expectedStatus": check.expected_status,
        "contentType": content_type,
        "expectedContentType": check.expected_content_type,
        "missingHeaders": missing_headers,
        "bodyPreview": " ".join(body.split())[:180],
        "ok": ok,
    }


def main() -> int:
    results = [run_check(check) for check in checks()]
    print(json.dumps({"results": results}, ensure_ascii=False, indent=2))
    failed = [result for result in results if not result["ok"]]
    if failed:
        print(f"Falharam {len(failed)} verificações de segurança.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
