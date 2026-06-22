package com.opiagile.supportai.chat;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.opiagile.supportai.lead.Intent;
import com.opiagile.supportai.rag.RetrievedChunk;

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
        if (intent == Intent.FALAR_COM_HUMANO) {
            return "Claro. Vou sinalizar esta conversa para atendimento humano e manter o contexto registrado para facilitar o retorno.";
        }
        if (intent == Intent.RECLAMACAO) {
            return "Sinto muito pelo problema relatado. Vou sinalizar esta conversa para atendimento humano com prioridade operacional.";
        }
        if (intent == Intent.FORA_DO_ESCOPO) {
            return "Não encontrei segurança para tratar essa solicitação dentro da base configurada. Vou recomendar encaminhamento para uma pessoa revisar.";
        }
        if (prompt.sources().isEmpty()) {
            return "Não encontrei informações suficientes na base de conhecimento para responder com segurança. Posso encaminhar esta conversa para atendimento humano.";
        }
        RetrievedChunk bestChunk = prompt.sources().getFirst();
        return "Encontrei uma informação relevante na base: " + bestChunk.excerpt()
                + " Posso ajudar com o próximo passo ou encaminhar para atendimento humano, se preferir.";
    }
}
