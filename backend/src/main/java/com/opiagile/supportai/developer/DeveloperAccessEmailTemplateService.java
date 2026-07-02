package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class DeveloperAccessEmailTemplateService {

    private static final String PUBLIC_CONTACT = "contato@opiagile.com";

    private final DeveloperAccessEmailOutboxRepository outboxRepository;
    private final DeveloperAccessEmailProperties properties;

    public DeveloperAccessEmailTemplateService(
            DeveloperAccessEmailOutboxRepository outboxRepository,
            DeveloperAccessEmailProperties properties) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
    }

    void enqueueRequestReceived(DeveloperAccessRequestRecord record, DeveloperAccessRequest request) {
        outboxRepository.enqueue(
                record.id(),
                DeveloperAccessEmailType.LEAD_REQUEST_CONFIRMATION,
                request.email(),
                "Recebemos sua solicitação - Opiagile",
                leadRequestConfirmationHtml(request),
                leadRequestConfirmationText(request));

        if (!properties.to().isBlank()) {
            outboxRepository.enqueue(
                    record.id(),
                    DeveloperAccessEmailType.ADMIN_NEW_REQUEST,
                    properties.to(),
                    properties.subjectPrefix() + " - " + fallback(request.company(), request.name()),
                    adminNewRequestHtml(record, request),
                    adminNewRequestText(record, request));
        }
    }

    void enqueueSandboxApproved(
            DeveloperAccessRequestAdminResponse request,
            DeveloperAccessApprovalResponse approval,
            String keyDeliveryUrl,
            OffsetDateTime keyDeliveryExpiresAt) {
        outboxRepository.enqueue(
                request.id(),
                DeveloperAccessEmailType.LEAD_SANDBOX_APPROVED,
                request.email(),
                "Seu acesso sandbox Opiagile foi aprovado",
                leadSandboxApprovedHtml(request, approval, keyDeliveryUrl, keyDeliveryExpiresAt),
                leadSandboxApprovedText(request, approval, keyDeliveryUrl, keyDeliveryExpiresAt));

        if (!properties.to().isBlank()) {
            outboxRepository.enqueue(
                    request.id(),
                    DeveloperAccessEmailType.ADMIN_SANDBOX_APPROVED,
                    properties.to(),
                    "[Opiagile] Sandbox aprovado - " + fallback(request.company(), request.email()),
                    adminSandboxApprovedHtml(request, approval),
                    adminSandboxApprovedText(request, approval));
        }
    }

    private String leadRequestConfirmationHtml(DeveloperAccessRequest request) {
        return layout(
                "Solicitação recebida",
                "Vamos avaliar seu acesso sandbox.",
                """
                        <p>Olá, %s.</p>
                        <p>Recebemos sua solicitação de acesso sandbox à API da Opiagile. Vamos avaliar o caso de uso informado e, se aprovado, enviaremos um link seguro para acessar a chave uma única vez.</p>
                        <div class="notice">
                          <strong>Importante:</strong> o sandbox é temporário e serve para validação controlada. Não envie dados sensíveis, dados pessoais reais ou documentos confidenciais durante o teste.
                        </div>
                        <p>Se precisar complementar a solicitação, responda este email ou fale com a Opiagile pelo endereço %s.</p>
                        """.formatted(escape(fallback(request.name(), "tudo bem")), PUBLIC_CONTACT));
    }

    private String leadRequestConfirmationText(DeveloperAccessRequest request) {
        return """
                Olá, %s.

                Recebemos sua solicitação de acesso sandbox à API da Opiagile.
                Se aprovado, enviaremos um link seguro para acessar a chave uma única vez.

                Importante: o sandbox é temporário e serve para validação controlada. Não envie dados sensíveis, dados pessoais reais ou documentos confidenciais durante o teste.

                Contato: %s
                """.formatted(fallback(request.name(), "tudo bem"), PUBLIC_CONTACT);
    }

    private String adminNewRequestHtml(DeveloperAccessRequestRecord record, DeveloperAccessRequest request) {
        return layout(
                "Nova solicitação developer",
                "Um lead pediu acesso sandbox.",
                """
                        <p>Uma nova solicitação de API key sandbox foi registrada no portal Developers.</p>
                        %s
                        <p class="muted">Próxima ação: avaliar a solicitação no painel interno e aprovar um sandbox temporário, se fizer sentido.</p>
                        """.formatted(detailsTable(
                        row("ID", record.id().toString()) +
                                row("Data", format(record.createdAt())) +
                                row("Nome", request.name()) +
                                row("Empresa", request.company()) +
                                row("Email", request.email()) +
                                row("Objetivo", request.useCase()) +
                                row("Recursos", request.requestedResources()))));
    }

    private String adminNewRequestText(DeveloperAccessRequestRecord record, DeveloperAccessRequest request) {
        return """
                Nova solicitação de API key sandbox.

                ID: %s
                Data: %s
                Nome: %s
                Empresa: %s
                Email: %s
                Objetivo: %s
                Recursos: %s
                """.formatted(
                record.id(),
                format(record.createdAt()),
                fallback(request.name(), "-"),
                fallback(request.company(), "-"),
                fallback(request.email(), "-"),
                fallback(request.useCase(), "-"),
                fallback(request.requestedResources(), "-"));
    }

    private String leadSandboxApprovedHtml(
            DeveloperAccessRequestAdminResponse request,
            DeveloperAccessApprovalResponse approval,
            String keyDeliveryUrl,
            OffsetDateTime keyDeliveryExpiresAt) {
        String expiration = approval.expiresAt() == null ? "conforme liberação informada" : format(approval.expiresAt());
        return layout(
                "Sandbox aprovado",
                "Use o link seguro para acessar sua chave uma única vez.",
                """
                        <p>Olá, %s.</p>
                        <p>Seu acesso sandbox à API da Opiagile foi aprovado. Por segurança, a API key não fica visível neste email.</p>
                        <p><a class="button" href="%s">Acessar API key uma única vez</a></p>
                        <div class="notice">
                          <strong>Atenção:</strong> o link de acesso à chave expira em %s e só pode ser usado uma vez.
                        </div>
                        %s
                        <p>Use a chave no header <code>X-OPIAGILE-API-KEY</code>. Para testar no console, acesse o portal Developers da Opiagile.</p>
                        <p class="muted">%s</p>
                        """.formatted(
                        escape(fallback(request.name(), "tudo bem")),
                        escape(keyDeliveryUrl),
                        escape(format(keyDeliveryExpiresAt)),
                        detailsTable(
                                row("Tenant", approval.tenantSlug()) +
                                        row("Workspace", approval.workspaceSlug()) +
                                        row("Escopos", scopes(approval.scopes())) +
                                        row("Limite por minuto", String.valueOf(approval.rateLimitPerMinute())) +
                                        row("Expiração do sandbox", expiration)),
                        escape(fallback(approval.retentionNotice(), "Não envie dados sensíveis durante o teste."))));
    }

    private String leadSandboxApprovedText(
            DeveloperAccessRequestAdminResponse request,
            DeveloperAccessApprovalResponse approval,
            String keyDeliveryUrl,
            OffsetDateTime keyDeliveryExpiresAt) {
        return """
                Olá, %s.

                Seu acesso sandbox à API da Opiagile foi aprovado.

                Acesse sua API key uma única vez:
                %s

                O link expira em: %s

                Tenant: %s
                Workspace: %s
                Escopos: %s
                Limite por minuto: %s
                Expiração do sandbox: %s

                Use a chave no header X-OPIAGILE-API-KEY.

                %s
                """.formatted(
                fallback(request.name(), "tudo bem"),
                keyDeliveryUrl,
                format(keyDeliveryExpiresAt),
                approval.tenantSlug(),
                approval.workspaceSlug(),
                scopes(approval.scopes()),
                approval.rateLimitPerMinute(),
                approval.expiresAt() == null ? "conforme liberação informada" : format(approval.expiresAt()),
                fallback(approval.retentionNotice(), "Não envie dados sensíveis durante o teste."));
    }

    private String adminSandboxApprovedHtml(DeveloperAccessRequestAdminResponse request, DeveloperAccessApprovalResponse approval) {
        return layout(
                "Sandbox aprovado",
                "A chave foi preparada para entrega segura ao lead.",
                """
                        <p>Um sandbox foi aprovado. O lead receberá um link de uso único para acessar a API key.</p>
                        %s
                        """.formatted(detailsTable(
                        row("Solicitação", request.id().toString()) +
                                row("Lead", fallback(request.name(), "-")) +
                                row("Empresa", fallback(request.company(), "-")) +
                                row("Email", request.email()) +
                                row("Tenant", approval.tenantSlug()) +
                                row("Workspace", approval.workspaceSlug()) +
                                row("Prefixo da chave", approval.keyPrefix()) +
                                row("Expiração", approval.expiresAt() == null ? "-" : format(approval.expiresAt())))));
    }

    private String adminSandboxApprovedText(DeveloperAccessRequestAdminResponse request, DeveloperAccessApprovalResponse approval) {
        return """
                Sandbox aprovado.

                Solicitação: %s
                Lead: %s
                Empresa: %s
                Email: %s
                Tenant: %s
                Workspace: %s
                Prefixo da chave: %s
                Expiração: %s
                """.formatted(
                request.id(),
                fallback(request.name(), "-"),
                fallback(request.company(), "-"),
                request.email(),
                approval.tenantSlug(),
                approval.workspaceSlug(),
                approval.keyPrefix(),
                approval.expiresAt() == null ? "-" : format(approval.expiresAt()));
    }

    private String layout(String title, String subtitle, String content) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                  <body style="margin:0;background:#f6f8f7;color:#18231f;font-family:Inter,Arial,sans-serif;">
                    <div style="display:none;max-height:0;overflow:hidden;">%s</div>
                    <main style="max-width:680px;margin:0 auto;padding:32px 18px;">
                      <section style="background:#ffffff;border:1px solid #dfe8e3;border-radius:18px;overflow:hidden;box-shadow:0 18px 55px rgba(24,35,31,.08);">
                        <header style="padding:26px 28px;background:#10251d;color:#ffffff;">
                          <div style="font-weight:800;letter-spacing:.02em;">Opiagile</div>
                          <h1 style="margin:22px 0 8px;font-size:28px;line-height:1.12;">%s</h1>
                          <p style="margin:0;color:#cfe5d8;font-size:15px;line-height:1.55;">%s</p>
                        </header>
                        <div style="padding:28px;font-size:15px;line-height:1.65;">
                          %s
                        </div>
                        <footer style="padding:20px 28px;border-top:1px solid #dfe8e3;color:#5d6b64;font-size:13px;line-height:1.5;">
                          Opiagile • conhecimento em respostas claras<br />
                          Este email foi enviado por %s.
                        </footer>
                      </section>
                    </main>
                  </body>
                </html>
                """.formatted(escape(subtitle), escape(title), escape(subtitle), content + styleHelpers(), PUBLIC_CONTACT);
    }

    private String styleHelpers() {
        return """
                <style>
                  .button{display:inline-block;background:#62a88b;color:#10251d!important;text-decoration:none;font-weight:800;padding:12px 16px;border-radius:10px}
                  .notice{background:#eef7f2;border:1px solid #cfe5d8;border-radius:12px;padding:14px 16px;margin:18px 0}
                  .muted{color:#5d6b64}
                  code{background:#eef2f0;border:1px solid #dfe8e3;border-radius:6px;padding:2px 5px}
                  table{width:100%;border-collapse:collapse;margin:18px 0}
                  td{border-bottom:1px solid #e8eeea;padding:9px 0;vertical-align:top}
                  td:first-child{color:#5d6b64;width:34%;padding-right:12px}
                </style>
                """;
    }

    private String detailsTable(String rows) {
        return "<table>" + rows + "</table>";
    }

    private String row(String label, String value) {
        return "<tr><td>" + escape(label) + "</td><td><strong>" + escape(fallback(value, "-")) + "</strong></td></tr>";
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
