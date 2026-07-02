const state = {
  spec: null,
  operations: [],
  selectedOperationId: null,
  apiKey: sessionStorage.getItem("opiagile_api_key") || "",
};

const elements = {
  serverSelect: document.querySelector("#server-select"),
  apiKeyInput: document.querySelector("#api-key-input"),
  toggleKey: document.querySelector("#toggle-key"),
  loadWorkspaces: document.querySelector("#load-workspaces"),
  workspaceResult: document.querySelector("#workspace-result"),
  search: document.querySelector("#operation-search"),
  operationList: document.querySelector("#operation-list"),
  statusBanner: document.querySelector("#status-banner"),
  operationPanel: document.querySelector("#operation-panel"),
  template: document.querySelector("#operation-template"),
};

const METHOD_ORDER = ["get", "post", "put", "patch", "delete"];

async function boot() {
  elements.apiKeyInput.value = state.apiKey;
  bindGlobalEvents();

  try {
    const response = await fetch("/v3/api-docs/rag-core", { headers: { Accept: "application/json" } });
    if (!response.ok) {
      throw new Error(`OpenAPI retornou HTTP ${response.status}`);
    }

    state.spec = await response.json();
    state.operations = collectOperations(state.spec);
    renderServers(state.spec.servers || []);
    renderOperations();
    setStatus(`${state.operations.length} endpoints carregados do contrato OpenAPI.`, false);
  } catch (error) {
    setStatus(`Não foi possível carregar o contrato OpenAPI: ${error.message}`, true);
  }
}

function bindGlobalEvents() {
  elements.apiKeyInput.addEventListener("input", (event) => {
    state.apiKey = event.target.value.trim();
    if (state.apiKey) {
      sessionStorage.setItem("opiagile_api_key", state.apiKey);
    } else {
      sessionStorage.removeItem("opiagile_api_key");
    }
    updateCurlPreview();
  });

  elements.toggleKey.addEventListener("click", () => {
    const nextType = elements.apiKeyInput.type === "password" ? "text" : "password";
    elements.apiKeyInput.type = nextType;
    elements.toggleKey.textContent = nextType === "password" ? "Ver" : "Ocultar";
  });

  elements.loadWorkspaces.addEventListener("click", loadWorkspaces);
  elements.search.addEventListener("input", renderOperations);
  elements.serverSelect.addEventListener("change", updateCurlPreview);
}

function setStatus(message, isError) {
  elements.statusBanner.textContent = message;
  elements.statusBanner.classList.toggle("is-error", Boolean(isError));
}

function renderServers(servers) {
  const currentOrigin = { url: window.location.origin, description: "Ambiente atual" };
  const normalized = [
    currentOrigin,
    ...servers.filter((server) => server.url !== currentOrigin.url && !isLocalhostServer(server.url)),
  ];
  elements.serverSelect.replaceChildren(
    ...normalized.map((server) => {
      const option = document.createElement("option");
      option.value = server.url;
      option.textContent = server.description ? `${server.url} - ${server.description}` : server.url;
      return option;
    }),
  );
}

async function loadWorkspaces() {
  elements.workspaceResult.classList.remove("is-error");
  if (!state.apiKey) {
    elements.workspaceResult.classList.add("is-error");
    elements.workspaceResult.textContent = "Cole uma API key antes de consultar os workspaces permitidos.";
    return;
  }

  elements.workspaceResult.textContent = "Consultando workspaces permitidos...";

  try {
    const baseUrl = elements.serverSelect.value.replace(/\/$/, "");
    const response = await fetch(`${baseUrl}/api/workspaces`, {
      headers: {
        Accept: "application/json",
        "X-OPIAGILE-API-KEY": state.apiKey,
      },
    });
    const text = await response.text();
    const workspaces = parseApiResponse(response, text);
    renderWorkspaceResult(workspaces);
  } catch (error) {
    elements.workspaceResult.classList.add("is-error");
    elements.workspaceResult.textContent = `Não foi possível carregar workspaces: ${error.message}`;
  }
}

function renderWorkspaceResult(workspaces) {
  if (!Array.isArray(workspaces) || !workspaces.length) {
    elements.workspaceResult.textContent = "Nenhum workspace retornado para esta chave.";
    return;
  }

  const list = document.createElement("ul");
  list.className = "workspace-list";

  for (const workspace of workspaces) {
    const item = document.createElement("li");
    const tenant = workspace.tenantSlug || workspace.tenant || "tenant";
    const slug = workspace.slug || workspace.workspaceSlug || "workspace";
    const name = workspace.name || slug;
    item.innerHTML = `
      <strong>${escapeHtml(name)}</strong>
      <span>tenant: ${escapeHtml(tenant)} / workspace: ${escapeHtml(slug)}</span>
    `;
    list.append(item);
  }

  elements.workspaceResult.replaceChildren(list);
}

function collectOperations(spec) {
  return Object.entries(spec.paths || {})
    .flatMap(([path, pathItem]) =>
      METHOD_ORDER.filter((method) => pathItem[method]).map((method) => {
        const operation = pathItem[method];
        const tag = operation.tags?.[0] || "outros";
        return {
          id: `${method.toUpperCase()} ${path}`,
          method,
          path,
          tag,
          summary: operation.summary || readablePath(path),
          description: stripMarkdown(operation.description || operation.summary || ""),
          parameters: [...(pathItem.parameters || []), ...(operation.parameters || [])],
          requestBody: operation.requestBody,
          operation,
        };
      }),
    )
    .sort((a, b) => {
      const tagCompare = a.tag.localeCompare(b.tag, "pt-BR");
      if (tagCompare !== 0) return tagCompare;
      return a.path.localeCompare(b.path, "pt-BR") || METHOD_ORDER.indexOf(a.method) - METHOD_ORDER.indexOf(b.method);
    });
}

function renderOperations() {
  const term = elements.search.value.trim().toLowerCase();
  const visibleOperations = state.operations.filter((operation) => {
    const haystack = `${operation.tag} ${operation.method} ${operation.path} ${operation.summary} ${operation.description}`.toLowerCase();
    return haystack.includes(term);
  });

  const groups = groupByTag(visibleOperations);
  const fragment = document.createDocumentFragment();

  for (const [tag, operations] of groups) {
    const group = document.createElement("section");
    group.className = "tag-group";

    const title = document.createElement("h2");
    title.className = "tag-title";
    title.textContent = formatTag(tag);
    group.append(title);

    for (const operation of operations) {
      const button = document.createElement("button");
      button.className = `operation-button ${operation.id === state.selectedOperationId ? "is-active" : ""}`;
      button.type = "button";
      button.innerHTML = `
        <span class="method-pill method-${operation.method}">${operation.method}</span>
        <span>
          <span class="operation-name">${escapeHtml(operation.summary)}</span>
          <span class="operation-path">${escapeHtml(operation.path)}</span>
        </span>
      `;
      button.addEventListener("click", () => selectOperation(operation.id));
      group.append(button);
    }

    fragment.append(group);
  }

  elements.operationList.replaceChildren(fragment);
}

function selectOperation(operationId) {
  state.selectedOperationId = operationId;
  renderOperations();
  const operation = state.operations.find((item) => item.id === operationId);
  if (!operation) return;
  renderOperationPanel(operation);
}

function renderOperationPanel(operation) {
  const node = elements.template.content.cloneNode(true);
  const card = node.querySelector(".request-card");
  const methodBadge = node.querySelector(".method-badge");
  const methodCode = node.querySelector(".method-code");
  const title = node.querySelector("h2");
  const description = node.querySelector(".operation-description");
  const pathCode = node.querySelector(".path-code");
  const paramsGrid = node.querySelector(".params-grid");
  const bodySection = node.querySelector(".body-section");
  const bodyEditor = node.querySelector(".body-editor");
  const multipartFields = node.querySelector(".multipart-fields");
  const form = node.querySelector(".request-form");
  const responseOutput = node.querySelector(".response-output");
  const responseMeta = node.querySelector(".response-meta");
  const curlPreview = node.querySelector(".curl-preview");
  const fillExample = node.querySelector(".fill-example");
  const copyCurl = node.querySelector(".copy-curl");
  const resetButton = node.querySelector(".reset-button");

  methodBadge.className = `method-badge method-${operation.method}`;
  methodCode.className = `method-code method-${operation.method}`;
  methodBadge.textContent = operation.method;
  methodCode.textContent = operation.method;
  title.textContent = operation.summary;
  description.textContent = operation.description || "Endpoint disponível no contrato público do RAG Core.";
  pathCode.textContent = operation.path;

  renderParameters(operation, paramsGrid);
  const bodyInfo = renderRequestBody(operation, bodySection, bodyEditor, multipartFields);

  const context = { operation, card, bodyInfo, curlPreview, responseOutput, responseMeta };
  const updateCurrentCurl = () => {
    curlPreview.textContent = buildCurl(context);
  };

  card.addEventListener("input", updateCurrentCurl);
  card.addEventListener("change", updateCurrentCurl);

  fillExample.addEventListener("click", () => {
    if (bodyInfo.kind === "json") {
      bodyEditor.value = JSON.stringify(bodyInfo.example, null, 2);
      updateCurrentCurl();
    }
  });

  copyCurl.addEventListener("click", async () => {
    await navigator.clipboard.writeText(curlPreview.textContent);
    copyCurl.textContent = "Copiado";
    window.setTimeout(() => {
      copyCurl.textContent = "Copiar";
    }, 1400);
  });

  resetButton.addEventListener("click", () => {
    responseMeta.textContent = "Aguardando execução";
    responseOutput.classList.remove("is-error", "is-success");
    responseOutput.textContent = 'Selecione "Executar endpoint" para ver status, tempo e corpo da resposta.';
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    await executeRequest(context);
  });

  elements.operationPanel.replaceChildren(node);
  updateCurrentCurl();
}

function renderParameters(operation, container) {
  const parameters = operation.parameters
    .map((parameter) => resolveRef(parameter))
    .filter((parameter) => parameter.in !== "header" || parameter.name.toLowerCase() !== "x-opiagile-api-key");

  if (!parameters.length) {
    const empty = document.createElement("p");
    empty.className = "hint";
    empty.textContent = "Este endpoint não possui parâmetros de path, query ou header além da API key.";
    container.append(empty);
    return;
  }

  for (const parameter of parameters) {
    const card = document.createElement("label");
    card.className = "field-card";
    const inputType = parameter.schema?.type === "integer" || parameter.schema?.type === "number" ? "number" : "text";
    card.innerHTML = `
      <span class="field-label">${escapeHtml(parameter.name)} ${parameter.required ? '<span class="required">*</span>' : ""}</span>
      <input class="control parameter-input" data-param-name="${escapeHtml(parameter.name)}" data-param-in="${escapeHtml(parameter.in)}" type="${inputType}" placeholder="${escapeHtml(parameter.in)}" />
      <p class="field-meta">${escapeHtml(parameter.description || `${parameter.in} parameter`)}</p>
    `;
    container.append(card);
  }
}

function renderRequestBody(operation, section, editor, multipartFields) {
  const requestBody = resolveRef(operation.requestBody);
  if (!requestBody?.content) {
    section.hidden = true;
    return { kind: "none", contentType: null, example: null };
  }

  const contentType = Object.keys(requestBody.content).find((item) => item.includes("json"))
    || Object.keys(requestBody.content).find((item) => item.includes("multipart"))
    || Object.keys(requestBody.content)[0];
  const media = requestBody.content[contentType] || {};
  const schema = resolveRef(media.schema);
  const example = media.example || media.examples?.default?.value || schemaExample(schema);

  section.hidden = false;

  if (contentType.includes("multipart")) {
    editor.hidden = true;
    section.querySelector(".fill-example").hidden = true;
    renderMultipartFields(schema, multipartFields);
    return { kind: "multipart", contentType, schema, example };
  }

  editor.hidden = false;
  multipartFields.replaceChildren();
  editor.value = JSON.stringify(example, null, 2);
  return { kind: "json", contentType, schema, example };
}

function renderMultipartFields(schema, container) {
  container.replaceChildren();
  const properties = schema?.properties && Object.keys(schema.properties).length ? schema.properties : { file: { type: "string", format: "binary" } };
  const required = new Set(schema?.required || []);

  for (const [name, rawProperty] of Object.entries(properties)) {
    const property = resolveRef(rawProperty);
    const isFile = property.format === "binary" || name.toLowerCase().includes("file");
    const label = document.createElement("label");
    label.className = "field-card";
    label.innerHTML = `
      <span class="field-label">${escapeHtml(name)} ${required.has(name) ? '<span class="required">*</span>' : ""}</span>
      <input class="control multipart-input" data-field-name="${escapeHtml(name)}" type="${isFile ? "file" : "text"}" ${isFile ? "" : `placeholder="${escapeHtml(property.description || name)}"`} />
      <p class="field-meta">${escapeHtml(property.description || (isFile ? "Arquivo enviado como multipart/form-data." : "Campo textual multipart."))}</p>
    `;
    container.append(label);
  }
}

function buildRequest(context, options = {}) {
  const { operation, card, bodyInfo } = context;
  const baseUrl = elements.serverSelect.value.replace(/\/$/, "");
  const query = new URLSearchParams();
  let path = operation.path;
  const headers = {};

  if (state.apiKey) {
    headers["X-OPIAGILE-API-KEY"] = state.apiKey;
  }

  for (const input of card.querySelectorAll(".parameter-input")) {
    const name = input.dataset.paramName;
    const location = input.dataset.paramIn;
    const value = input.value.trim();
    if (!value) continue;
    if (location === "path") {
      path = path.replace(`{${name}}`, encodeURIComponent(value));
    } else if (location === "query") {
      query.set(name, value);
    } else if (location === "header") {
      headers[name] = value;
    }
  }

  const url = `${baseUrl}${path}${query.toString() ? `?${query.toString()}` : ""}`;
  if (options.validatePath && /{[^}]+}/.test(path)) {
    throw new Error("Preencha todos os parâmetros obrigatórios do path antes de executar.");
  }

  let body = null;

  if (bodyInfo.kind === "json") {
    const rawBody = card.querySelector(".body-editor").value.trim();
    if (rawBody) {
      body = rawBody;
      headers["Content-Type"] = bodyInfo.contentType || "application/json";
    }
  } else if (bodyInfo.kind === "multipart") {
    body = new FormData();
    for (const input of card.querySelectorAll(".multipart-input")) {
      const name = input.dataset.fieldName;
      if (input.type === "file") {
        if (input.files[0]) body.append(name, input.files[0]);
      } else if (input.value.trim()) {
        body.append(name, input.value.trim());
      }
    }
  }

  return { url, headers, body };
}

function buildCurl(context) {
  const { operation } = context;
  const request = buildRequest(context);
  const lines = [`curl -X ${operation.method.toUpperCase()} "${request.url}"`];

  for (const [name, value] of Object.entries(request.headers)) {
    const displayValue = name.toLowerCase() === "x-opiagile-api-key" ? maskSecret(value) : value;
    lines.push(`  -H "${name}: ${displayValue}"`);
  }

  if (request.body instanceof FormData) {
    for (const [name, value] of request.body.entries()) {
      lines.push(`  -F "${name}=${value instanceof File ? `@${value.name || "arquivo"}` : value}"`);
    }
  } else if (request.body) {
    lines.push(`  -d '${request.body}'`);
  }

  return lines.join(" \\\n");
}

async function executeRequest(context) {
  const { operation, responseOutput, responseMeta } = context;
  responseOutput.classList.remove("is-error", "is-success");
  responseOutput.textContent = "Executando...";
  responseMeta.textContent = "Requisição em andamento";

  try {
    const request = buildRequest(context, { validatePath: true });
    if (request.body && !(request.body instanceof FormData) && request.headers["Content-Type"]?.includes("json")) {
      JSON.parse(request.body);
    }

    const startedAt = performance.now();
    const response = await fetch(request.url, {
      method: operation.method.toUpperCase(),
      headers: request.headers,
      body: ["get", "delete"].includes(operation.method) ? null : request.body,
    });
    const duration = Math.round(performance.now() - startedAt);
    const text = await response.text();
    const formattedBody = formatResponseBody(text, response.headers.get("content-type"), response.status);

    responseMeta.textContent = `HTTP ${response.status} em ${duration} ms`;
    responseOutput.classList.add(response.ok ? "is-success" : "is-error");
    responseOutput.textContent = formattedBody || "(sem corpo de resposta)";
  } catch (error) {
    responseMeta.textContent = "Erro antes ou durante a execução";
    responseOutput.classList.add("is-error");
    responseOutput.textContent = error.message;
  }
}

function updateCurlPreview() {
  const selected = state.selectedOperationId;
  if (!selected) return;
  const curlPreview = document.querySelector(".curl-preview");
  const card = document.querySelector(".request-card");
  const operation = state.operations.find((item) => item.id === selected);
  if (!curlPreview || !card || !operation) return;
  const bodyEditor = card.querySelector(".body-editor");
  const multipartFields = card.querySelector(".multipart-fields");
  const bodyInfo = bodyEditor && !bodyEditor.hidden
    ? { kind: "json", contentType: "application/json" }
    : multipartFields?.children.length
      ? { kind: "multipart", contentType: "multipart/form-data" }
      : { kind: "none" };
  curlPreview.textContent = buildCurl({ operation, card, bodyInfo });
}

function schemaExample(schema, depth = 0) {
  const resolved = resolveRef(schema);
  if (!resolved || depth > 5) return {};
  if (resolved.example !== undefined) return resolved.example;
  if (resolved.default !== undefined) return resolved.default;
  if (resolved.enum?.length) return resolved.enum[0];

  if (resolved.allOf?.length) {
    return Object.assign({}, ...resolved.allOf.map((item) => schemaExample(item, depth + 1)));
  }

  if (resolved.oneOf?.length || resolved.anyOf?.length) {
    return schemaExample((resolved.oneOf || resolved.anyOf)[0], depth + 1);
  }

  if (resolved.type === "array") {
    return [schemaExample(resolved.items, depth + 1)];
  }

  if (resolved.type === "object" || resolved.properties) {
    const output = {};
    for (const [name, property] of Object.entries(resolved.properties || {})) {
      output[name] = schemaExample(property, depth + 1);
    }
    return output;
  }

  if (resolved.format === "uuid") return "00000000-0000-0000-0000-000000000000";
  if (resolved.format === "date-time") return new Date().toISOString();
  if (resolved.type === "integer") return 0;
  if (resolved.type === "number") return 0;
  if (resolved.type === "boolean") return false;
  return "string";
}

function resolveRef(value) {
  if (!value?.$ref) return value;
  return value.$ref
    .replace("#/", "")
    .split("/")
    .reduce((current, part) => current?.[part], state.spec);
}

function parseApiResponse(response, text) {
  const contentType = response.headers.get("content-type") || "";
  const parsed = parseResponseBody(text, contentType);
  if (!response.ok) {
    throw new Error(responseErrorMessage(response.status, parsed));
  }
  if (typeof parsed === "string") {
    if (looksLikeHtml(parsed)) {
      throw new Error("A rota respondeu HTML em vez de JSON. Verifique o proxy público para encaminhar este endpoint ao RAG Core.");
    }
    throw new Error("A API retornou texto em vez de JSON.");
  }
  return parsed;
}

function parseResponseBody(text, contentType) {
  if (!text) return "";
  if (contentType?.includes("json")) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  if (looksLikeJson(text)) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  return text;
}

function formatResponseBody(text, contentType, status) {
  const parsed = parseResponseBody(text, contentType);
  if (!text) return "";
  if (typeof parsed === "string") {
    if (looksLikeHtml(parsed)) {
      return `HTTP ${status}: a rota respondeu HTML em vez de JSON. Verifique se este endpoint está publicado no proxy do RAG Core.`;
    }
    return parsed;
  }
  return JSON.stringify(parsed, null, 2);
}

function responseErrorMessage(status, data) {
  if (data && typeof data === "object") {
    return data.detail || data.message || data.error || `Erro HTTP ${status}`;
  }
  if (typeof data === "string" && looksLikeHtml(data)) {
    return `HTTP ${status}: a rota respondeu HTML em vez de JSON. O endpoint provavelmente não está encaminhado para o RAG Core.`;
  }
  return data || `Erro HTTP ${status}`;
}

function looksLikeJson(value) {
  return /^\s*[\[{]/.test(value || "");
}

function looksLikeHtml(value) {
  return /^\s*<!doctype html|^\s*<html|<body[\s>]/i.test(value || "");
}

function maskSecret(value) {
  if (!value) return "";
  if (value.length <= 8) return "********";
  return `${value.slice(0, 4)}...${value.slice(-4)}`;
}

function readablePath(path) {
  return path
    .split("/")
    .filter(Boolean)
    .map((part) => part.replace(/[{}-]/g, " "))
    .join(" ");
}

function formatTag(tag) {
  return tag
    .replace("-controller", "")
    .replace(/-/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function isLocalhostServer(url) {
  try {
    const parsed = new URL(url, window.location.origin);
    return ["localhost", "127.0.0.1", "::1"].includes(parsed.hostname);
  } catch {
    return /localhost|127\.0\.0\.1/.test(url);
  }
}

function groupByTag(operations) {
  const groups = new Map();
  for (const operation of operations) {
    const current = groups.get(operation.tag) || [];
    current.push(operation);
    groups.set(operation.tag, current);
  }
  return groups;
}

function stripMarkdown(value) {
  return value
    .replace(/`([^`]+)`/g, "$1")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/\s+/g, " ")
    .trim();
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

boot();
