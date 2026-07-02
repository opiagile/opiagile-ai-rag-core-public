window.addEventListener("load", () => {
  window.ui = SwaggerUIBundle({
    url: "/v3/api-docs/rag-core",
    dom_id: "#swagger-ui",
    deepLinking: true,
    displayRequestDuration: true,
    docExpansion: "none",
    filter: true,
    operationsSorter: "method",
    tagsSorter: "alpha",
    tryItOutEnabled: true,
    persistAuthorization: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset,
    ],
    layout: "BaseLayout",
  });
});
