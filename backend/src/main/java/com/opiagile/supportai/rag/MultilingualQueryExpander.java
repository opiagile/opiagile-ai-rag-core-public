package com.opiagile.supportai.rag;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class MultilingualQueryExpander {

    private static final Map<String, Set<String>> TERM_TRANSLATIONS = Map.ofEntries(
            Map.entry("service", Set.of("servico", "servicos", "solucao", "solucoes")),
            Map.entry("services", Set.of("servico", "servicos", "solucao", "solucoes")),
            Map.entry("offer", Set.of("oferta", "oferece", "servicos")),
            Map.entry("offers", Set.of("oferta", "oferece", "servicos")),
            Map.entry("help", Set.of("ajuda", "apoiar", "orientar")),
            Map.entry("company", Set.of("empresa", "negocio")),
            Map.entry("business", Set.of("empresa", "negocio")),
            Map.entry("knowledge", Set.of("conhecimento", "documentos", "base")),
            Map.entry("document", Set.of("documento", "documentos")),
            Map.entry("documents", Set.of("documento", "documentos", "manuais")),
            Map.entry("manual", Set.of("manual", "manuais")),
            Map.entry("faq", Set.of("faq", "perguntas", "duvidas")),
            Map.entry("question", Set.of("pergunta", "duvida")),
            Map.entry("questions", Set.of("perguntas", "duvidas")),
            Map.entry("answer", Set.of("resposta", "respostas")),
            Map.entry("answers", Set.of("resposta", "respostas")),
            Map.entry("whatsapp", Set.of("whatsapp", "canal", "integracao")),
            Map.entry("teams", Set.of("teams", "microsoft", "canal", "integracao")),
            Map.entry("slack", Set.of("slack", "canal", "integracao")),
            Map.entry("integration", Set.of("integracao", "integracoes")),
            Map.entry("integrations", Set.of("integracao", "integracoes", "canais")),
            Map.entry("security", Set.of("seguranca", "lgpd", "dados")),
            Map.entry("privacy", Set.of("privacidade", "lgpd", "dados")),
            Map.entry("data", Set.of("dados", "informacao")),
            Map.entry("contact", Set.of("contato", "email", "telefone")),
            Map.entry("person", Set.of("pessoa", "atendente", "especialista")),
            Map.entry("human", Set.of("humano", "pessoa", "atendente")),
            Map.entry("specialist", Set.of("especialista", "pessoa")),
            Map.entry("proposal", Set.of("proposta", "orcamento", "comercial")),
            Map.entry("meeting", Set.of("reuniao", "contato", "comercial")),
            Map.entry("send", Set.of("enviar", "informar", "nome", "empresa", "email", "telefone", "resumo")),
            Map.entry("information", Set.of("informacao", "dados", "nome", "empresa", "email", "telefone", "resumo")),
            Map.entry("demo", Set.of("demo", "demonstracao")),
            Map.entry("cost", Set.of("custo", "preco", "valor")),
            Map.entry("price", Set.of("preco", "valor", "orcamento")),
            Map.entry("rental", Set.of("locacao", "aluguel")),
            Map.entry("rent", Set.of("locacao", "aluguel")),
            Map.entry("property", Set.of("imovel")),
            Map.entry("visit", Set.of("visita", "visitar")),
            Map.entry("broker", Set.of("corretor")),
            Map.entry("analysis", Set.of("analise")),
            Map.entry("cancel", Set.of("cancelamento", "cancelar", "rescisao", "encerramento")),
            Map.entry("cancellation", Set.of("cancelamento", "cancelar", "rescisao", "encerramento")),
            Map.entry("termination", Set.of("rescisao", "encerramento", "contrato")),
            Map.entry("servicio", Set.of("servico", "servicos", "solucao", "solucoes")),
            Map.entry("servicios", Set.of("servico", "servicos", "solucao", "solucoes")),
            Map.entry("ayuda", Set.of("ajuda", "apoiar", "orientar")),
            Map.entry("empresa", Set.of("empresa", "negocio")),
            Map.entry("negocio", Set.of("empresa", "negocio")),
            Map.entry("conocimiento", Set.of("conhecimento", "documentos", "base")),
            Map.entry("documento", Set.of("documento", "documentos")),
            Map.entry("documentos", Set.of("documento", "documentos", "manuais")),
            Map.entry("manuales", Set.of("manual", "manuais")),
            Map.entry("pregunta", Set.of("pergunta", "duvida")),
            Map.entry("preguntas", Set.of("perguntas", "duvidas")),
            Map.entry("respuesta", Set.of("resposta", "respostas")),
            Map.entry("respuestas", Set.of("resposta", "respostas")),
            Map.entry("integracion", Set.of("integracao", "integracoes")),
            Map.entry("integraciones", Set.of("integracao", "integracoes", "canais")),
            Map.entry("seguridad", Set.of("seguranca", "lgpd", "dados")),
            Map.entry("privacidad", Set.of("privacidade", "lgpd", "dados")),
            Map.entry("datos", Set.of("dados", "informacao")),
            Map.entry("contacto", Set.of("contato", "email", "telefone")),
            Map.entry("persona", Set.of("pessoa", "atendente", "especialista")),
            Map.entry("humano", Set.of("humano", "pessoa", "atendente")),
            Map.entry("especialista", Set.of("especialista", "pessoa")),
            Map.entry("propuesta", Set.of("proposta", "orcamento", "comercial")),
            Map.entry("reunion", Set.of("reuniao", "contato", "comercial")),
            Map.entry("enviar", Set.of("enviar", "informar", "nome", "empresa", "email", "telefone", "resumo")),
            Map.entry("informacion", Set.of("informacao", "dados", "nome", "empresa", "email", "telefone", "resumo")),
            Map.entry("demostracion", Set.of("demo", "demonstracao")),
            Map.entry("costo", Set.of("custo", "preco", "valor")),
            Map.entry("precio", Set.of("preco", "valor", "orcamento")),
            Map.entry("alquiler", Set.of("locacao", "aluguel")),
            Map.entry("inmueble", Set.of("imovel")),
            Map.entry("corredor", Set.of("corretor")),
            Map.entry("analisis", Set.of("analise")),
            Map.entry("cancelacion", Set.of("cancelamento", "cancelar", "rescisao")),
            Map.entry("rescision", Set.of("rescisao", "encerramento", "contrato")));

    public String expand(String query, String responseLanguage) {
        if (query == null || query.isBlank()) {
            return query;
        }
        if ("PORTUGUESE".equals(responseLanguage)) {
            return query;
        }

        Set<String> additions = terms(query).stream()
                .flatMap(term -> TERM_TRANSLATIONS.getOrDefault(term, Set.of()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (additions.isEmpty()) {
            return query;
        }
        return query + " " + String.join(" ", additions);
    }

    private Set<String> terms(String value) {
        return Arrays.stream(normalize(value).split("[^a-z0-9]+"))
                .filter(term -> term.length() >= 3)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }
}
