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
        String language = prompt.responseLanguage();
        if (intent == Intent.FALAR_COM_HUMANO) {
            if ("ENGLISH".equals(language)) {
                return "Sure. I will flag this conversation for human support and keep the context registered to make the follow-up easier.";
            }
            if ("SPANISH".equals(language)) {
                return "Claro. Voy a marcar esta conversación para atención humana y mantener el contexto registrado para facilitar el retorno.";
            }
            return "Claro. Vou sinalizar esta conversa para atendimento humano e manter o contexto registrado para facilitar o retorno.";
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
                return "I do not have enough confidence to handle this request with the configured knowledge base. I recommend forwarding it to a person for review.";
            }
            if ("SPANISH".equals(language)) {
                return "No tengo suficiente seguridad para tratar esta solicitud con la base configurada. Recomiendo enviarla a una persona para revisión.";
            }
            return "Não encontrei segurança para tratar essa solicitação dentro da base configurada. Vou recomendar encaminhamento para uma pessoa revisar.";
        }
        if (prompt.sources().isEmpty()) {
            if ("ENGLISH".equals(language)) {
                return "I did not find enough information in the knowledge base to answer safely. I can forward this conversation to human support.";
            }
            if ("SPANISH".equals(language)) {
                return "No encontré información suficiente en la base de conocimiento para responder con seguridad. Puedo enviar esta conversación a atención humana.";
            }
            return "Não encontrei informações suficientes na base de conhecimento para responder com segurança. Posso encaminhar esta conversa para atendimento humano.";
        }
        RetrievedChunk bestChunk = prompt.sources().getFirst();
        if ("ENGLISH".equals(language)) {
            return "I found relevant information in the knowledge base: " + bestChunk.excerpt()
                    + " I can help with the next step or forward this to human support if you prefer.";
        }
        if ("SPANISH".equals(language)) {
            return "Encontré información relevante en la base: " + bestChunk.excerpt()
                    + " Puedo ayudarte con el siguiente paso o enviarlo a atención humana si lo prefieres.";
        }
        return "Encontrei uma informação relevante na base: " + bestChunk.excerpt()
                + " Posso ajudar com o próximo passo ou encaminhar para atendimento humano, se preferir.";
    }
}
