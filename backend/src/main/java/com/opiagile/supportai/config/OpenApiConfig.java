package com.opiagile.supportai.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    public static final String API_KEY_SCHEME = "OpiagileApiKey";

    @Bean
    OpenAPI opiagileOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Opiagile RAG Core API")
                        .version("v0.11")
                        .description("""
                                API HTTP para integrar aplicações, portais, gateways e automações ao core RAG da Opiagile.

                                Use esta documentação para validar ingestão de documentos, chat com fontes,
                                workspaces, observabilidade e handoff. Endpoints protegidos exigem o header
                                `X-OPIAGILE-API-KEY`.
                                """)
                        .contact(new Contact()
                                .name("Opiagile")
                                .email("contato@opiagile.com")
                                .url("https://opiagile.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://github.com/opiagile/opiagile-ai-rag-core/blob/main/LICENSE")))
                .servers(List.of(
                        new Server().url("https://opiagile.com").description("Ambiente de demonstração controlada")))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-OPIAGILE-API-KEY")
                                .description("API key tenant-aware fornecida pela Opiagile para apps, gateways e integrações.")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME))
                .tags(List.of(
                        new Tag().name("chat-controller").description("Chat RAG com fontes, memória e LLM opcional."),
                        new Tag().name("document-controller").description("Ingestão e consulta de documentos por workspace."),
                        new Tag().name("workspace-controller").description("Workspaces disponíveis para uma chave ou tenant."),
                        new Tag().name("conversation-controller").description("Histórico e conversas para auditoria operacional."),
                        new Tag().name("observability-controller").description("Trace de recuperação, fontes e resposta."),
                        new Tag().name("handoff-controller").description("Encaminhamento para atendimento humano."),
                        new Tag().name("external-tool-controller").description("Ferramentas controladas por workspace."),
                        new Tag().name("provider-controller").description("Status seguro de provedores IA e fallback."),
                        new Tag().name("version-controller").description("Metadados públicos da aplicação.")));
    }

    @Bean
    GroupedOpenApi ragCoreApi() {
        return GroupedOpenApi.builder()
                .group("rag-core")
                .pathsToMatch(
                        "/api/chat",
                        "/api/documents/**",
                        "/api/workspaces/**",
                        "/api/conversations/**",
                        "/api/observability/**",
                        "/api/handoffs/**",
                        "/api/tools/**",
                        "/api/providers/**",
                        "/api/version")
                .pathsToExclude(
                        "/api/admin/**",
                        "/api/site-chat/**",
                        "/api/webhooks/**")
                .build();
    }
}
