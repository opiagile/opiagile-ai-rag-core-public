package com.opiagile.supportai.chat;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.opiagile.supportai.lead.Intent;
import com.opiagile.supportai.rag.RetrievedChunk;
import com.opiagile.supportai.tool.ToolExecutionResult;

@Component
public class DemoChatModelProvider {

    public ChatGenerationResult generate(ChatPrompt prompt) {
        Instant startedAt = Instant.now();
        String answer = buildAnswer(prompt);
        return ChatGenerationResult.demo(answer, prompt.fallbackReason(),
                Duration.between(startedAt, Instant.now()).toMillis());
    }

    private String buildAnswer(ChatPrompt prompt) {
        Intent intent = prompt.intent();
        String language = prompt.responseLanguage();
        if (isDemoRequest(prompt.currentMessage())) {
            return demoAnswer(language);
        }
        if (intent == Intent.FALAR_COM_HUMANO) {
            if ("ENGLISH".equals(language)) {
                return "Sure. I can forward this request to a person. To help us contact you properly, please send your name, company, email or phone, and a short summary of what you need.";
            }
            if ("SPANISH".equals(language)) {
                return "Claro. Puedo enviar esta solicitud a una persona. Para que podamos contactarte correctamente, informa tu nombre, empresa, email o teléfono y un breve resumen de lo que necesitas.";
            }
            return "Claro. Posso encaminhar essa solicitação para uma pessoa. Para entrarmos em contato corretamente, informe seu nome, empresa, email ou telefone e um breve resumo do que você precisa.";
        }
        if (intent == Intent.RECLAMACAO) {
            if ("ENGLISH".equals(language)) {
                return "I am sorry about the reported issue. I will flag this conversation for human support with operational priority.";
            }
            if ("SPANISH".equals(language)) {
                return "Siento mucho el problema reportado. Voy a marcar esta conversación para atención humana con prioridad operativa.";
            }
            return "Sinto muito pelo problema relatado. Vou sinalizar esta conversa para atendimento humano com prioridade operacional.";
        }
        if (intent == Intent.FORA_DO_ESCOPO) {
            if ("ENGLISH".equals(language)) {
                return "I do not have enough reliable information to handle this request here. If you want a specialist to review it, please send your name, company, email or phone, and a short summary of the context.";
            }
            if ("SPANISH".equals(language)) {
                return "No tengo información suficiente y confiable para tratar esta solicitud aquí. Si quieres que un especialista la revise, informa tu nombre, empresa, email o teléfono y un breve resumen del contexto.";
            }
            return "Não encontrei informação segura o suficiente para tratar essa solicitação por aqui. Se quiser que um especialista avalie, informe seu nome, empresa, email ou telefone e um breve resumo do contexto.";
        }
        if (!prompt.toolResults().isEmpty()) {
            return toolResultAnswer(prompt.toolResults().getFirst(), language);
        }
        if (prompt.sources().isEmpty()) {
            if ("ENGLISH".equals(language)) {
                return "I did not find indexed sources in this workspace with enough information to answer safely. You can upload a TXT knowledge base for this demo area or ask for human support.";
            }
            if ("SPANISH".equals(language)) {
                return "No encontré fuentes indexadas en este workspace con información suficiente para responder con seguridad. Puedes subir una base TXT para esta área de demo o pedir atención humana.";
            }
            return "Não encontrei fontes indexadas neste workspace com informação suficiente para responder com segurança. Você pode enviar uma base TXT para esta área da demo ou pedir atendimento humano.";
        }
        RetrievedChunk bestChunk = prompt.sources().getFirst();
        String normalizedMessage = normalize(prompt.currentMessage());
        if (isIntegrationQuestion(normalizedMessage)) {
            return integrationAnswer(language);
        }
        if (isBusinessValueQuestion(normalizedMessage)) {
            return businessValueAnswer(language);
        }
        if ("ENGLISH".equals(language)) {
            return "Based on this workspace knowledge base, " + evidenceText(bestChunk)
                    + " If this requires a real booking, approval or operational change, I should route it to a person.";
        }
        if ("SPANISH".equals(language)) {
            return "Según la base de conocimiento de este workspace, " + evidenceText(bestChunk)
                    + " Si esto requiere agenda real, aprobación o cambio operativo, debo encaminarlo a una persona.";
        }
        return "Pela base de conhecimento deste workspace, " + evidenceText(bestChunk)
                + " Se isso exigir agendamento real, aprovação ou mudança operacional, devo encaminhar para uma pessoa.";
    }

    private boolean isDemoRequest(String message) {
        if (message == null) {
            return false;
        }
        String normalized = java.text.Normalizer.normalize(message.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.contains("testar")
                || normalized.contains("validar")
                || normalized.contains("demo")
                || normalized.contains("demonstracao")
                || normalized.contains("experimentar")
                || normalized.contains("ver funcionando")
                || normalized.contains("online test")
                || normalized.contains("try")
                || normalized.contains("demo")
                || normalized.contains("probar")
                || normalized.contains("validar");
    }

    private String demoAnswer(String language) {
        if ("ENGLISH".equals(language)) {
            return "Yes. You can test the Opiagile RAG demo at https://demo-rag.opiagile.com. The demo lets you upload a document, ask questions in natural language, see answers with sources and understand how separate workspaces can support different use cases.";
        }
        if ("SPANISH".equals(language)) {
            return "Sí. Puedes probar la demo RAG de Opiagile en https://demo-rag.opiagile.com. La demo permite subir un documento, hacer preguntas en lenguaje natural, ver respuestas con fuentes y entender cómo los espacios separados apoyan diferentes casos de uso.";
        }
        return "Sim. Você pode testar a demo RAG da Opiagile em https://demo-rag.opiagile.com. A demo permite subir um documento, fazer perguntas em linguagem natural, ver respostas com fontes e entender como workspaces separados apoiam diferentes casos de uso.";
    }

    private boolean isIntegrationQuestion(String normalizedMessage) {
        return containsAny(normalizedMessage,
                "integrar",
                "integracao",
                "api",
                "banco de dados",
                "base de dados",
                "sistema interno",
                "whatsapp",
                "teams",
                "slack",
                "crm",
                "salesforce",
                "hubspot",
                "webhook");
    }

    private boolean isBusinessValueQuestion(String normalizedMessage) {
        return containsAny(normalizedMessage,
                "como pode ajudar",
                "ajudar minha empresa",
                "ajudar meu negocio",
                "o que a opiagile faz",
                "beneficio",
                "beneficios",
                "problema resolve",
                "para que serve");
    }

    private String integrationAnswer(String language) {
        if ("ENGLISH".equals(language)) {
            return "Yes. Opiagile can be integrated with web chat, WhatsApp, CRM, internal systems, APIs and databases when the use case requires it. The recommended approach is to keep a clear scope, use secure credentials, apply minimum permissions and expose only controlled data to the assistant.";
        }
        if ("SPANISH".equals(language)) {
            return "Sí. Opiagile puede integrarse con chat web, WhatsApp, CRM, sistemas internos, APIs y bases de datos cuando el caso de uso lo requiere. La recomendación es definir un alcance claro, usar credenciales seguras, aplicar permisos mínimos y exponer solo datos controlados al asistente.";
        }
        return "Sim. A Opiagile pode ser integrada com chat web, WhatsApp, CRM, sistemas internos, APIs e bancos de dados quando o caso de uso exigir. O caminho recomendado é definir um escopo claro, usar credenciais seguras, aplicar permissões mínimas e expor ao assistente apenas dados controlados.";
    }

    private String businessValueAnswer(String language) {
        if ("ENGLISH".equals(language)) {
            return "Opiagile helps companies turn documents, FAQs, manuals and internal knowledge into fast answers with sources. This reduces rework, improves service consistency and makes knowledge easier for teams, customers and partners to access.";
        }
        if ("SPANISH".equals(language)) {
            return "Opiagile ayuda a las empresas a transformar documentos, preguntas frecuentes, manuales y conocimiento interno en respuestas rápidas con fuentes. Esto reduce retrabajo, mejora la consistencia de la atención y facilita el acceso al conocimiento para equipos, clientes y socios.";
        }
        return "A Opiagile ajuda empresas a transformar documentos, FAQs, manuais e conhecimento interno em respostas rápidas com fontes. Isso reduz retrabalho, melhora a consistência do atendimento e torna o conhecimento mais acessível para equipes, clientes e parceiros.";
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String cleanExcerpt(String excerpt) {
        if (excerpt == null || excerpt.isBlank()) {
            return "há informação relevante na base de conhecimento.";
        }
        String cleaned = excerpt.replaceAll("\\s+", " ").trim();
        while (cleaned.startsWith(".")) {
            cleaned = cleaned.substring(1).trim();
        }
        if (cleaned.length() <= 420) {
            return cleaned;
        }
        int sentenceEnd = cleaned.indexOf(". ");
        if (sentenceEnd > 80 && sentenceEnd < 420) {
            return cleaned.substring(0, sentenceEnd + 1);
        }
        return cleaned.substring(0, 420).trim() + "...";
    }

    private String evidenceText(RetrievedChunk chunk) {
        String excerpt = chunk.excerpt();
        if (excerpt != null && !excerpt.strip().startsWith("...")) {
            return cleanExcerpt(excerpt);
        }
        return cleanExcerpt(chunk.content());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }

    private String toolResultAnswer(ToolExecutionResult result, String language) {
        if (!"SUCCESS".equals(result.status())) {
            if ("ENGLISH".equals(language)) {
                return "I could not consult the controlled workspace data safely in this request.";
            }
            if ("SPANISH".equals(language)) {
                return "No pude consultar de forma segura los datos controlados del workspace en esta solicitud.";
            }
            return "Não consegui consultar os dados controlados do workspace com segurança nesta solicitação.";
        }
        if ("ENGLISH".equals(language)) {
            return "I checked the controlled workspace data and found " + result.rowCount()
                    + " result row(s). Main data: " + result.rows();
        }
        if ("SPANISH".equals(language)) {
            return "Consulté los datos controlados del workspace y encontré " + result.rowCount()
                    + " fila(s) de resultado. Datos principales: " + result.rows();
        }
        return "Consultei os dados controlados do workspace e encontrei " + result.rowCount()
                + " linha(s) de resultado. Dados principais: " + result.rows();
    }
}
