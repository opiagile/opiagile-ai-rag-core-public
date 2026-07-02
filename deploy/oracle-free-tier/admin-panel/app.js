const tokenInput = document.querySelector("#admin-token");
const rememberToken = document.querySelector("#remember-token");
const clearToken = document.querySelector("#clear-token");
const statusFilter = document.querySelector("#status-filter");
const loadButton = document.querySelector("#load-requests");
const requestsList = document.querySelector("#requests-list");
const approvalForm = document.querySelector("#approval-form");
const requestIdInput = document.querySelector("#request-id");
const clientNameInput = document.querySelector("#client-name");
const tenantSlugInput = document.querySelector("#tenant-slug");
const workspaceSelect = document.querySelector("#workspace-slug");
const workspaceHelp = document.querySelector("#workspace-help");
const loadWorkspacesButton = document.querySelector("#load-workspaces");
const approvalModeInputs = document.querySelectorAll('input[name="approvalMode"]');
const existingWorkspaceFields = document.querySelector("#existing-workspace-fields");
const temporarySandboxFields = document.querySelector("#temporary-sandbox-fields");
const keyDialog = document.querySelector("#key-dialog");
const generatedKey = document.querySelector("#generated-key");
const generatedMetadata = document.querySelector("#generated-metadata");
const copyKeyButton = document.querySelector("#copy-key");
const toast = document.querySelector("#toast");

const STORAGE_KEY = "opiagile-admin-token";
let workspacesLoadedForTenant = "";

const storedToken = localStorage.getItem(STORAGE_KEY);
if (storedToken) {
  tokenInput.value = storedToken;
  rememberToken.checked = true;
}

rememberToken.addEventListener("change", () => {
  if (rememberToken.checked && tokenInput.value.trim()) {
    localStorage.setItem(STORAGE_KEY, tokenInput.value.trim());
    showToast("Token mantido neste navegador.");
    return;
  }
  localStorage.removeItem(STORAGE_KEY);
});

tokenInput.addEventListener("input", () => {
  if (rememberToken.checked) {
    localStorage.setItem(STORAGE_KEY, tokenInput.value.trim());
  }
  if (tokenInput.value.trim()) {
    queueWorkspaceLoad();
  }
});

clearToken.addEventListener("click", () => {
  tokenInput.value = "";
  rememberToken.checked = false;
  localStorage.removeItem(STORAGE_KEY);
  showToast("Token removido.");
});

loadButton.addEventListener("click", loadRequests);
approvalForm.addEventListener("submit", approveRequest);
tenantSlugInput.addEventListener("change", loadWorkspaces);
tenantSlugInput.addEventListener("blur", loadWorkspaces);
loadWorkspacesButton.addEventListener("click", loadWorkspaces);
approvalModeInputs.forEach((input) => input.addEventListener("change", syncApprovalMode));
copyKeyButton.addEventListener("click", async () => {
  await navigator.clipboard.writeText(generatedKey.textContent.trim());
  showToast("Chave copiada.");
});
syncApprovalMode();

async function loadRequests() {
  requestsList.innerHTML = '<p class="muted">Carregando solicitações...</p>';
  try {
    if (approvalMode() === "existing") {
      await ensureWorkspacesLoaded();
    }
    const status = encodeURIComponent(statusFilter.value);
    const requests = await adminFetch(`/api/admin/developer-access-requests?status=${status}&limit=50`);
    if (!requests.length) {
      requestsList.innerHTML = '<p class="muted">Nenhuma solicitação encontrada para este filtro.</p>';
      return;
    }
    requestsList.innerHTML = "";
    requests.forEach((request) => requestsList.appendChild(renderRequest(request)));
  } catch (error) {
    requestsList.innerHTML = `<p class="error">${escapeHtml(error.message)}</p>`;
  }
}

async function loadWorkspaces() {
  const tenant = stringValue(tenantSlugInput.value || "demo") || "demo";
  if (!tokenInput.value.trim()) {
    workspaceHelp.textContent = "Informe o token administrativo para carregar workspaces.";
    return;
  }
  workspaceSelect.disabled = true;
  loadWorkspacesButton.disabled = true;
  workspaceHelp.textContent = "Carregando workspaces...";
  try {
    const workspaces = await adminFetch(`/api/admin/developer-access-requests/workspaces?tenant=${encodeURIComponent(tenant)}`);
    workspaceSelect.innerHTML = "";
    if (!workspaces.length) {
      workspaceHelp.textContent = "Nenhum workspace encontrado para este tenant. A aprovação falhará se o workspace não existir.";
      const option = document.createElement("option");
      option.value = "";
      option.textContent = "Nenhum workspace disponível";
      workspaceSelect.appendChild(option);
      return;
    }
    workspaces.forEach((workspace) => {
      const option = document.createElement("option");
      option.value = workspace.workspaceSlug;
      option.textContent = `${workspace.workspaceName || workspace.workspaceSlug} (${workspace.workspaceSlug})`;
      option.dataset.tenantSlug = workspace.tenantSlug;
      workspaceSelect.appendChild(option);
    });
    workspacesLoadedForTenant = tenant;
    workspaceHelp.textContent = "Selecione um workspace existente. A aprovação não cria workspaces novos.";
  } catch (error) {
    workspaceHelp.textContent = `Não foi possível carregar workspaces: ${error.message}`;
  } finally {
    workspaceSelect.disabled = false;
    loadWorkspacesButton.disabled = false;
  }
}

async function ensureWorkspacesLoaded() {
  const tenant = stringValue(tenantSlugInput.value || "demo") || "demo";
  if (workspacesLoadedForTenant !== tenant) {
    await loadWorkspaces();
  }
}

function queueWorkspaceLoad() {
  window.clearTimeout(queueWorkspaceLoad.timeout);
  queueWorkspaceLoad.timeout = window.setTimeout(() => {
    if (tokenInput.value.trim()) {
      loadWorkspaces();
    }
  }, 450);
}

async function approveRequest(event) {
  event.preventDefault();
  const formData = new FormData(approvalForm);
  const requestId = String(formData.get("requestId") || "").trim();
  if (!requestId) {
    showToast("Informe o ID da solicitação.");
    return;
  }

  const scopes = Array.from(approvalForm.querySelectorAll('input[name="scope"]:checked')).map((input) => input.value);
  const mode = approvalMode();
  const payload = {
    tenantSlug: stringValue(formData.get("tenantSlug")),
    workspaceSlug: stringValue(formData.get("workspaceSlug")),
    scopes,
    rateLimitPerMinute: Number(formData.get("rateLimitPerMinute") || 30),
    clientName: stringValue(formData.get("clientName")),
    expiresInHours: Number(formData.get("expiresInHours") || 24),
  };

  if (mode === "existing" && !payload.workspaceSlug) {
    showToast("Selecione um workspace existente antes de aprovar.");
    return;
  }

  try {
    const endpoint =
      mode === "temporary"
        ? `/api/admin/developer-access-requests/${encodeURIComponent(requestId)}/approve-temporary-sandbox`
        : `/api/admin/developer-access-requests/${encodeURIComponent(requestId)}/approve`;
    const result = await adminFetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    generatedKey.textContent = result.apiKey;
    generatedMetadata.innerHTML = renderGeneratedMetadata(result);
    keyDialog.showModal();
    showToast("Solicitação aprovada.");
    await loadRequests();
  } catch (error) {
    showToast(error.message);
  }
}

function renderRequest(request) {
  const article = document.createElement("article");
  article.className = "request";
  const approved = String(request.status || "").toUpperCase() === "APPROVED";
  article.innerHTML = `
    <header>
      <div>
        <h3>${escapeHtml(request.company || request.name || request.email)}</h3>
        <p class="muted">${escapeHtml(request.email || "")}</p>
      </div>
      <span class="badge">${escapeHtml(request.status || "NEW")}</span>
    </header>
    <dl>
      <dt>ID</dt>
      <dd>${escapeHtml(request.id)}</dd>
      <dt>Nome</dt>
      <dd>${escapeHtml(request.name || "-")}</dd>
      <dt>Uso</dt>
      <dd>${escapeHtml(request.useCase || "-")}</dd>
      <dt>Recursos</dt>
      <dd>${escapeHtml(request.requestedResources || "-")}</dd>
      <dt>Email</dt>
      <dd>${request.notificationEmailSent ? "Enviado" : "Pendente"} (${Number(request.notificationEmailAttempts || 0)} tentativas)</dd>
      <dt>Sandbox</dt>
      <dd>${sandboxSummary(request)}</dd>
    </dl>
    ${
      approved
        ? '<p class="muted">Solicitação já aprovada. Não é possível reemitir a chave.</p>'
        : '<button type="button" class="ghost">Usar na aprovação</button>'
    }
  `;
  const action = article.querySelector("button");
  if (action) {
    action.addEventListener("click", () => {
      requestIdInput.value = request.id;
      clientNameInput.value = request.company ? `Sandbox - ${request.company}` : "";
      approvalForm.querySelector('input[name="approvalMode"][value="temporary"]').checked = true;
      syncApprovalMode();
      document.querySelector("#approval").scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }
  return article;
}

function approvalMode() {
  return approvalForm.querySelector('input[name="approvalMode"]:checked')?.value || "temporary";
}

function syncApprovalMode() {
  const temporary = approvalMode() === "temporary";
  temporarySandboxFields.hidden = !temporary;
  existingWorkspaceFields.hidden = temporary;
  workspaceSelect.required = !temporary;
}

function renderGeneratedMetadata(result) {
  const rows = [
    ["Tenant", result.tenantSlug],
    ["Workspace", result.workspaceSlug],
    ["Expira em", result.expiresAt ? formatDate(result.expiresAt) : "Sem expiração automática"],
    ["Limite", `${result.rateLimitPerMinute || "-"} req/min`],
  ];
  const retention = result.retentionNotice
    ? `<p class="muted">${escapeHtml(result.retentionNotice)}</p>`
    : "";
  return `
    <dl>
      ${rows
        .map(([label, value]) => `<dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value || "-")}</dd>`)
        .join("")}
    </dl>
    ${retention}
  `;
}

function sandboxSummary(request) {
  const tenant = request.approvedTenantSlug || "-";
  const workspace = request.approvedWorkspaceSlug || "-";
  const expires = request.sandboxExpiresAt ? `Expira: ${formatDate(request.sandboxExpiresAt)}` : "Sem expiração";
  const deleted = request.sandboxDeletedAt ? `Removido: ${formatDate(request.sandboxDeletedAt)}` : "";
  return escapeHtml(`${tenant} / ${workspace}. ${expires}${deleted ? ". " + deleted : ""}`);
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(value));
  } catch {
    return value;
  }
}

async function adminFetch(path, options = {}) {
  const token = tokenInput.value.trim();
  if (!token) {
    throw new Error("Informe o token administrativo.");
  }
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.headers || {}),
      "X-Demo-Admin-Token": token,
    },
  });
  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  if (!response.ok) {
    const message = errorMessage(response.status, data);
    throw new Error(message);
  }
  return data;
}

function errorMessage(status, data) {
  if (data && typeof data === "object") {
    if (data.detail) {
      return data.detail;
    }
    if (data.message) {
      return data.message;
    }
    if (data.error) {
      if (status === 400) {
        return `${data.error}. Verifique tenant, workspace e campos obrigatórios.`;
      }
      if (status === 409) {
        return "Solicitação já aprovada. A chave não pode ser exibida novamente.";
      }
      if (status === 401) {
        return "Token administrativo ausente ou inválido.";
      }
      return data.error;
    }
  }
  if (status === 400) {
    return "Erro de validação. Verifique tenant, workspace e campos obrigatórios.";
  }
  if (status === 409) {
    return "Solicitação já aprovada. A chave não pode ser exibida novamente.";
  }
  if (status === 401) {
    return "Token administrativo ausente ou inválido.";
  }
  return `Erro HTTP ${status}`;
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("visible");
  window.clearTimeout(showToast.timeout);
  showToast.timeout = window.setTimeout(() => toast.classList.remove("visible"), 2600);
}

function stringValue(value) {
  return String(value || "").trim();
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

if (tokenInput.value.trim()) {
  loadWorkspaces();
}
