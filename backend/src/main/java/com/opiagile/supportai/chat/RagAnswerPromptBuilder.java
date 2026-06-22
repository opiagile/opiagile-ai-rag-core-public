package com.opiagile.supportai.chat;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.opiagile.supportai.conversation.MessageRecord;
import com.opiagile.supportai.rag.RetrievedChunk;

@Component
public class RagAnswerPromptBuilder {

    private final String tone;

    public RagAnswerPromptBuilder(@Value("${chat.system-tone:profissional, claro, acolhedor e objetivo}") String tone) {
        this.tone = tone;
    }

    public String instructions() {
        return """
                Você é o assistente de atendimento da Opiagile em uma demonstração de atendimento com IA/RAG.
                Responda sempre em português do Brasil, com tom %s.
                Use somente as fontes recuperadas e o histórico fornecido como base factual.
                Não invente informações, horários, preços, políticas, disponibilidade ou confirmação de agendamento.
                Se não houver fonte suficiente, diga de forma natural que não encontrou informação segura e ofereça encaminhamento humano.
                Não mencione termos técnicos para o usuário final, como chunk, score, retrieval, embedding, provider ou prompt.
                Não diga que foi treinado nos documentos e não se apresente como pessoa humana.
                Faça uma pergunta curta de próximo passo quando isso ajudar o atendimento.
                Responda em até 2 ou 3 parágrafos curtos.
                """.formatted(tone);
    }

    public String input(ChatPrompt prompt) {
        return """
                Contexto operacional:
                - Intenção detectada: %s
                - Status do lead: %s
                - Handoff requerido: %s
                - Motivo de fallback/handoff: %s

                Histórico recente:
                %s

                Fontes recuperadas:
                %s

                Mensagem atual do usuário:
                %s

                Gere apenas a resposta final para o usuário.
                """.formatted(
                prompt.intent().name(),
                prompt.leadStatus(),
                prompt.handoffRequired(),
                valueOrNone(prompt.fallbackReason()),
                renderHistory(prompt.recentMessages()),
                renderSources(prompt.sources()),
                prompt.currentMessage());
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

    private String valueOrNone(String value) {
        if (value == null || value.isBlank()) {
            return "nenhum";
        }
        return value;
    }
}
