package com.opiagile.supportai.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.opiagile.supportai.conversation.MessageRecord;
import com.opiagile.supportai.rag.RetrievedChunk;
import com.opiagile.supportai.tool.ExternalToolRecord;
import com.opiagile.supportai.tool.ToolExecutionResult;

@Component
public class RagAnswerPromptBuilder {

    private final String tone;

    public RagAnswerPromptBuilder(@Value("${chat.system-tone:profissional, claro, acolhedor e objetivo}") String tone) {
        this.tone = tone;
    }

    public String instructions() {
        return """
                Você é o assistente de atendimento da Opiagile em uma demonstração de atendimento com IA/RAG.
                A resposta final DEVE ser escrita inteira no idioma obrigatório informado no input, com tom %s.
                Se a mensagem do usuário ou as fontes estiverem em outro idioma, traduza naturalmente a resposta final para o idioma obrigatório.
                Não responda no idioma da mensagem do usuário quando ele for diferente do idioma obrigatório.
                Use somente as fontes recuperadas e o histórico fornecido como base factual.
                Não use conhecimento geral do modelo, memória prévia, matemática mental, senso comum ou fatos externos para completar respostas.
                Perguntas de conhecimento geral, curiosidades, física, matemática, programação, tradução ou qualquer tema que não esteja explicitamente coberto pelas fontes devem receber uma recusa curta: diga que não encontrou essa informação na base de conhecimento disponível.
                Não invente informações, horários, preços, políticas, disponibilidade ou confirmação de agendamento.
                Se não houver fonte suficiente, diga de forma natural que não encontrou informação segura e ofereça encaminhamento humano.
                Quando o usuário perguntar se pode testar, validar, ver demo, experimentar ou conhecer na prática, responda apontando para https://demo-rag.opiagile.com e explique brevemente o que a demo permite fazer. Nessa situação, não peça dados de contato, não ofereça proposta, não ofereça reunião e não ofereça acompanhamento humano; no máximo pergunte se o usuário quer orientação para começar pela demo.
                Só solicite nome, empresa, email ou telefone quando o usuário pedir claramente uma pessoa, especialista, proposta, orçamento, reunião, contato comercial ou análise do caso dele. Em respostas informativas, não repita pedido de contato nem insista em email.
                Se o usuário demonstrar interesse comercial, mas ainda estiver apenas tirando dúvidas, responda a dúvida e ofereça contato como opção secundária em uma frase curta.
                Não mencione termos técnicos para o usuário final, como chunk, score, retrieval, embedding, provider ou prompt.
                Quando houver ferramentas controladas disponíveis e o usuário perguntar sobre consultas a banco, analytics, sistemas internos ou dados operacionais, explique a capacidade em linguagem de negócio e deixe claro que o uso depende de permissões, escopo, credenciais seguras e regras de acesso. Não invente resultados de ferramentas que não foram executadas.
                Quando houver resultados de ferramentas executadas, use esses resultados como contexto factual adicional. Não diga que executou SQL e não mostre nomes de tabelas, a menos que o usuário peça detalhes técnicos.
                Não diga que foi treinado nos documentos e não se apresente como pessoa humana.
                Faça uma pergunta curta de próximo passo somente quando faltar um dado essencial para continuar.
                Se o usuário já confirmou, autorizou, disse que está de acordo, pediu para seguir ou informou que não tem mais nada a acrescentar, encerre com uma confirmação curta e não faça nova pergunta.
                Em agendamentos, nunca diga que o horário foi confirmado; diga apenas que a solicitação será encaminhada para confirmação da equipe.
                Responda em até 2 ou 3 parágrafos curtos.
                """.formatted(tone);
    }

    public String input(ChatPrompt prompt) {
        return """
                REGRA OBRIGATÓRIA DE IDIOMA:
                %s

                Contexto operacional:
                - Intenção detectada: %s
                - Status do lead: %s
                - Idioma obrigatório da resposta: %s
                - Handoff requerido: %s
                - Motivo de fallback/handoff: %s

                Histórico recente:
                %s

                Fontes recuperadas:
                %s

                Ferramentas controladas disponíveis no workspace:
                %s

                Resultados de ferramentas executadas nesta rodada:
                %s

                Mensagem atual do usuário:
                %s

                Antes de responder, confira a regra obrigatória de idioma. A resposta final deve estar somente nesse idioma, mesmo que a mensagem e as fontes estejam em outro idioma.
                Confira também se as fontes recuperadas respondem diretamente a mensagem atual. Se as fontes forem sobre outro assunto, não responda com conhecimento geral; informe que essa informação não foi encontrada na base de conhecimento disponível.
                Gere apenas a resposta final para o usuário.
                """.formatted(
                languageInstruction(prompt.responseLanguage()),
                prompt.intent().name(),
                prompt.leadStatus(),
                languageInstruction(prompt.responseLanguage()),
                prompt.handoffRequired(),
                valueOrNone(prompt.fallbackReason()),
                renderHistory(prompt.recentMessages()),
                renderSources(prompt.sources()),
                renderTools(prompt.availableTools()),
                renderToolResults(prompt.toolResults()),
                prompt.currentMessage());
    }

    private String languageInstruction(String responseLanguage) {
        return switch (responseLanguage) {
            case "ENGLISH" -> "ENGLISH ONLY. The final answer must be entirely in English. Do not use Portuguese or Spanish words. Translate terms such as 'Entendido', 'atendente humano', 'segunda a sexta' and 'horas úteis' into natural English.";
            case "SPANISH" -> "SPANISH. La respuesta final debe estar completamente en español.";
            default -> "PORTUGUESE. A resposta final deve estar completamente em português do Brasil.";
        };
    }

    private String renderHistory(List<MessageRecord> messages) {
        if (messages.isEmpty()) {
            return "- Sem histórico anterior.";
        }
        return messages.stream()
                .map(message -> "- " + message.role() + ": " + message.content())
                .collect(Collectors.joining("\n"));
    }

    private String renderSources(List<RetrievedChunk> sources) {
        if (sources.isEmpty()) {
            return "- Nenhuma fonte relevante foi recuperada.";
        }
        return sources.stream()
                .map(source -> "- Arquivo: " + source.filename()
                        + " | Documento: " + source.documentId()
                        + " | Trecho: " + source.excerpt())
                .collect(Collectors.joining("\n"));
    }

    private String renderTools(List<ExternalToolRecord> tools) {
        if (tools == null || tools.isEmpty()) {
            return "- Nenhuma ferramenta controlada disponível.";
        }
        String rendered = tools.stream()
                .filter(tool -> "ACTIVE".equals(tool.status()))
                .map(tool -> "- " + tool.name()
                        + " | Tipo: " + tool.type()
                        + " | Uso: " + tool.description())
                .collect(Collectors.joining("\n"));
        if (rendered.isBlank()) {
            return "- Nenhuma ferramenta controlada ativa.";
        }
        return rendered;
    }

    private String renderToolResults(List<ToolExecutionResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return "- Nenhuma ferramenta foi executada nesta rodada.";
        }
        return toolResults.stream()
                .map(result -> {
                    if (!"SUCCESS".equals(result.status())) {
                        return "- Ferramenta: " + result.toolSlug()
                                + " | Objetivo: " + result.purpose()
                                + " | Status: " + result.status()
                                + " | Erro: " + valueOrNone(result.errorMessage());
                    }
                    return "- Ferramenta: " + result.toolSlug()
                            + " | Objetivo: " + result.purpose()
                            + " | Status: " + result.status()
                            + " | Linhas: " + result.rowCount()
                            + " | Colunas: " + String.join(", ", result.columns())
                            + " | Resultado: " + result.rows();
                })
                .collect(Collectors.joining("\n"));
    }

    private String valueOrNone(String value) {
        if (value == null || value.isBlank()) {
            return "nenhum";
        }
        return value;
    }
}
