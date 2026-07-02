package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@Controller
public class DeveloperAccessKeyDeliveryController {

    private final DeveloperAccessKeyDeliveryService deliveryService;

    public DeveloperAccessKeyDeliveryController(DeveloperAccessKeyDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping(value = "/developers/access-key/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> show(@PathVariable String token) {
        if (!deliveryService.available(token)) {
            return html(page(
                    "Link indisponível",
                    "Este link expirou, já foi utilizado ou não é válido.",
                    """
                            <p>Por segurança, a API key só pode ser exibida uma vez.</p>
                            <p>Se você precisa de um novo acesso, solicite uma nova liberação no portal Developers ou fale com a Opiagile pelo email contato@opiagile.com.</p>
                            <p><a class="button secondary" href="/developers">Voltar ao portal Developers</a></p>
                            """));
        }
        return html(page(
                "Acesso sandbox aprovado",
                "Revele sua API key apenas quando estiver pronto para copiar e guardar com segurança.",
                """
                        <div class="notice">
                          <strong>Atenção:</strong> esta página não mostra a chave automaticamente. O botão abaixo exibe a API key uma única vez e invalida este link.
                        </div>
                        <form method="post">
                          <button class="button" type="submit">Revelar API key uma única vez</button>
                        </form>
                        <p class="muted">Não compartilhe a chave publicamente. Use apenas em ambiente de validação sandbox.</p>
                        """));
    }

    @PostMapping(value = "/developers/access-key/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> reveal(@PathVariable String token) {
        return deliveryService.reveal(token)
                .map(key -> html(page(
                        "API key revelada",
                        "Copie agora. Esta chave não será exibida novamente por este link.",
                        """
                                <label>API key</label>
                                <pre class="secret">%s</pre>
                                %s
                                <p>Use a chave no header <code>X-OPIAGILE-API-KEY</code>.</p>
                                <p><a class="button secondary" href="/developers/api-console/">Abrir console de API</a></p>
                                <div class="notice">
                                  <strong>LGPD e segurança:</strong> não envie dados sensíveis, dados pessoais reais ou documentos confidenciais durante testes sandbox.
                                </div>
                                """.formatted(escape(key.apiKey()), details(key)))))
                .orElseGet(() -> html(page(
                        "Link indisponível",
                        "Este link expirou, já foi utilizado ou não é válido.",
                        """
                                <p>Por segurança, a API key só pode ser exibida uma vez.</p>
                                <p>Solicite uma nova liberação se precisar continuar o teste.</p>
                                <p><a class="button secondary" href="/developers">Voltar ao portal Developers</a></p>
                                """)));
    }

    private String details(DeveloperAccessKeyDeliveryService.RevealedDeveloperAccessKey key) {
        return """
                <table>
                  <tr><td>Prefixo</td><td>%s</td></tr>
                  <tr><td>Tenant</td><td>%s</td></tr>
                  <tr><td>Workspace</td><td>%s</td></tr>
                  <tr><td>Escopos</td><td>%s</td></tr>
                  <tr><td>Limite por minuto</td><td>%s</td></tr>
                  <tr><td>Expiração do sandbox</td><td>%s</td></tr>
                  <tr><td>Link válido até</td><td>%s</td></tr>
                </table>
                <p class="muted">%s</p>
                """.formatted(
                escape(key.keyPrefix()),
                escape(key.tenantSlug()),
                escape(key.workspaceSlug()),
                escape(scopes(key.scopes())),
                key.rateLimitPerMinute(),
                escape(format(key.sandboxExpiresAt())),
                escape(format(key.deliveryExpiresAt())),
                escape(fallback(key.retentionNotice(), "O sandbox é temporário e os dados de teste são removidos ao expirar.")));
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(body);
    }

    private String page(String title, String subtitle, String content) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                  <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <title>%s | Opiagile</title>
                    <style>
                      :root{color-scheme:light;--green:#62a88b;--ink:#10251d;--muted:#5d6b64;--line:#dfe8e3;--bg:#f6f8f7}
                      *{box-sizing:border-box} body{margin:0;background:var(--bg);color:var(--ink);font-family:Inter,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
                      main{min-height:100vh;display:grid;place-items:center;padding:32px 16px}
                      section{width:min(760px,100%%);background:#fff;border:1px solid var(--line);border-radius:24px;box-shadow:0 24px 70px rgba(16,37,29,.10);overflow:hidden}
                      header{background:var(--ink);color:#fff;padding:28px}
                      .brand{display:flex;align-items:center;gap:10px;font-weight:850}.brand img{width:34px;height:34px;object-fit:contain}
                      h1{margin:28px 0 8px;font-size:clamp(30px,4vw,48px);line-height:1.02;letter-spacing:0}
                      header p{margin:0;color:#d8eadf;max-width:620px;line-height:1.55}
                      .content{padding:28px;font-size:15px;line-height:1.65}.notice{background:#eef7f2;border:1px solid #cfe5d8;border-radius:14px;padding:14px 16px;margin:18px 0}
                      .button{display:inline-flex;align-items:center;justify-content:center;border:0;border-radius:12px;background:var(--green);color:#10251d;font-weight:850;padding:13px 16px;text-decoration:none;cursor:pointer;font-size:15px}.button.secondary{background:#eef2f0}
                      label{display:block;color:var(--muted);font-size:13px;font-weight:750;margin-bottom:8px}.secret{white-space:pre-wrap;word-break:break-all;background:#0f1f19;color:#e8fff2;border-radius:14px;padding:16px;font-size:14px;line-height:1.5}
                      table{width:100%%;border-collapse:collapse;margin:18px 0}td{border-bottom:1px solid var(--line);padding:10px 0;vertical-align:top}td:first-child{color:var(--muted);width:34%%;padding-right:12px}.muted{color:var(--muted)}
                    </style>
                  </head>
                  <body>
                    <main>
                      <section>
                        <header>
                          <div class="brand"><img src="/developers/opiagile-logo.png" alt="" /> Opiagile</div>
                          <h1>%s</h1>
                          <p>%s</p>
                        </header>
                        <div class="content">%s</div>
                      </section>
                    </main>
                  </body>
                </html>
                """.formatted(escape(title), escape(title), escape(subtitle), content);
    }

    private String scopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "-";
        }
        return String.join(", ", scopes);
    }

    private String format(OffsetDateTime value) {
        if (value == null) {
            return "-";
        }
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String escape(String value) {
        return fallback(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
