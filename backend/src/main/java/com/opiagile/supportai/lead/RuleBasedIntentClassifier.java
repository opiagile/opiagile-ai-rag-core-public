package com.opiagile.supportai.lead;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "intent.llm.enabled", havingValue = "false", matchIfMissing = true)
public class RuleBasedIntentClassifier implements IntentClassifier {

    @Override
    public Intent classify(String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return Intent.DESCONHECIDO;
        }
        if (containsAny(normalized, "humano", "atendente", "pessoa", "falar com alguem")) {
            return Intent.FALAR_COM_HUMANO;
        }
        if (containsAny(normalized, "reclamacao", "reclamar", "problema grave", "insatisfeito", "insatisfeita")) {
            return Intent.RECLAMACAO;
        }
        if (containsAny(normalized, "remarcar", "reagendar", "mudar horario", "alterar horario")) {
            return Intent.REMARCAR;
        }
        if (containsAny(normalized, "cancelar", "cancelamento", "desmarcar")) {
            return Intent.CANCELAR;
        }
        if (containsAny(normalized, "agendar", "marcar", "consulta", "horario", "visita")) {
            return Intent.AGENDAR;
        }
        if (containsAny(normalized, "preco", "valor", "orcamento", "proposta", "contratar", "plano")) {
            return Intent.COMERCIAL;
        }
        if (containsAny(normalized, "piada", "receita", "futebol", "filme", "musica", "cirurgia", "emergencia", "urgencia", "pronto socorro")) {
            return Intent.FORA_DO_ESCOPO;
        }
        if (containsAny(normalized, "qual", "quais", "como", "quando", "onde", "documento", "atende", "sabado", "prazo")) {
            return Intent.DUVIDA_FAQ;
        }
        return Intent.DESCONHECIDO;
    }

    private boolean containsAny(String message, String... terms) {
        for (String term : terms) {
            if (message.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }
}
