package com.opiagile.supportai.rag;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class TextSimilarityScorer {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "como", "que", "por", "das", "dos", "nas", "nos",
            "qual", "quais", "quando", "onde", "sobre", "voce", "voces", "isso", "esta",
            "what", "which", "when", "where", "about", "should", "could", "would", "with",
            "the", "and", "for", "this", "that", "need", "needs", "how", "does",
            "cual", "cuales", "donde", "este", "debo");

    private static final Map<String, Set<String>> MULTILINGUAL_SYNONYMS = Map.ofEntries(
            Map.entry("information", Set.of("informacao", "informacoes", "dados", "detalhes")),
            Map.entry("info", Set.of("informacao", "informacoes", "dados")),
            Map.entry("send", Set.of("enviar", "envio", "informe", "informar")),
            Map.entry("provide", Set.of("informar", "informe", "enviar")),
            Map.entry("request", Set.of("solicitacao", "pedido")),
            Map.entry("issue", Set.of("problema", "solicitacao")),
            Map.entry("specialist", Set.of("especialista", "responsavel", "tecnica", "financeira", "comercial")),
            Map.entry("human", Set.of("humano", "humana", "pessoa", "atendente")),
            Map.entry("agent", Set.of("atendente", "pessoa", "humano")),
            Map.entry("support", Set.of("atendimento", "suporte")),
            Map.entry("contact", Set.of("contato", "telefone", "email")),
            Map.entry("name", Set.of("nome")),
            Map.entry("company", Set.of("empresa")),
            Map.entry("department", Set.of("departamento")),
            Map.entry("summary", Set.of("resumo")),
            Map.entry("response", Set.of("retorno", "resposta")),
            Map.entry("time", Set.of("prazo", "horario", "tempo")),
            Map.entry("deadline", Set.of("prazo", "retorno")),
            Map.entry("saturday", Set.of("sabado", "sabados")),
            Map.entry("appointment", Set.of("consulta", "agendamento", "agendar")),
            Map.entry("schedule", Set.of("agendar", "agendamento", "consulta")),
            Map.entry("documents", Set.of("documentos")),
            Map.entry("document", Set.of("documento")),
            Map.entry("rental", Set.of("locacao", "aluguel")),
            Map.entry("rent", Set.of("locacao", "aluguel")),
            Map.entry("property", Set.of("imovel")),
            Map.entry("visit", Set.of("visita", "visitar")),
            Map.entry("broker", Set.of("corretor")),
            Map.entry("analysis", Set.of("analise")),
            Map.entry("informacion", Set.of("informacao", "informacoes", "dados", "detalhes")),
            Map.entry("enviar", Set.of("informar", "informe")),
            Map.entry("solicitud", Set.of("solicitacao", "pedido")),
            Map.entry("especialista", Set.of("responsavel", "tecnica", "financeira", "comercial")),
            Map.entry("humano", Set.of("pessoa", "atendente")),
            Map.entry("atendente", Set.of("atendimento", "humano")),
            Map.entry("contacto", Set.of("contato", "telefone", "email")),
            Map.entry("nombre", Set.of("nome")),
            Map.entry("empresa", Set.of("departamento")),
            Map.entry("resumen", Set.of("resumo")),
            Map.entry("respuesta", Set.of("retorno", "resposta")),
            Map.entry("plazo", Set.of("prazo", "retorno")),
            Map.entry("sabados", Set.of("sabado", "sabados")),
            Map.entry("agendar", Set.of("agendamento", "consulta")),
            Map.entry("alquiler", Set.of("locacao", "aluguel")),
            Map.entry("inmueble", Set.of("imovel")),
            Map.entry("corredor", Set.of("corretor")),
            Map.entry("analisis", Set.of("analise")));

    public double score(String query, String content) {
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty() || content == null || content.isBlank()) {
            return 0.0;
        }
        Set<String> contentTerms = terms(content);
        long matches = queryTerms.stream()
                .filter(term -> matches(term, contentTerms))
                .count();
        return matches / (double) queryTerms.size();
    }

    public String excerpt(String query, String content, int maxChars) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String compactContent = content.replaceAll("\\s+", " ").trim();
        int start = firstTermPosition(query, compactContent);
        int safeStart = start < 0 ? 0 : Math.max(0, start - maxChars / 4);
        safeStart = adjustExcerptStart(compactContent, safeStart);
        int safeEnd = Math.min(compactContent.length(), safeStart + maxChars);
        safeEnd = adjustExcerptEnd(compactContent, safeStart, safeEnd);
        String excerpt = compactContent.substring(safeStart, safeEnd).trim();
        if (safeStart > 0) {
            excerpt = "..." + excerpt;
        }
        if (safeEnd < compactContent.length()) {
            excerpt = excerpt + "...";
        }
        return excerpt;
    }

    private int adjustExcerptEnd(String content, int safeStart, int safeEnd) {
        if (safeEnd >= content.length() || safeEnd <= safeStart) {
            return safeEnd;
        }
        if (!Character.isLetterOrDigit(content.charAt(safeEnd - 1)) || !Character.isLetterOrDigit(content.charAt(safeEnd))) {
            return safeEnd;
        }
        int previousSpace = content.lastIndexOf(' ', safeEnd);
        return previousSpace <= safeStart ? safeEnd : previousSpace;
    }

    private int adjustExcerptStart(String content, int safeStart) {
        if (safeStart == 0 || !Character.isLetterOrDigit(content.charAt(safeStart))) {
            return safeStart;
        }
        if (!Character.isLetterOrDigit(content.charAt(safeStart - 1))) {
            return safeStart;
        }
        int nextSpace = content.indexOf(' ', safeStart);
        return nextSpace < 0 ? safeStart : nextSpace + 1;
    }

    private int firstTermPosition(String query, String content) {
        String normalizedContent = normalize(content);
        return terms(query).stream()
                .flatMap(term -> expandedTerms(term).stream())
                .map(normalizedContent::indexOf)
                .filter(index -> index >= 0)
                .min(Integer::compareTo)
                .orElse(-1);
    }

    private boolean matches(String queryTerm, Set<String> contentTerms) {
        return expandedTerms(queryTerm).stream().anyMatch(contentTerms::contains);
    }

    private Set<String> expandedTerms(String term) {
        Set<String> expanded = new HashSet<>();
        expanded.add(term);
        expanded.addAll(MULTILINGUAL_SYNONYMS.getOrDefault(term, Set.of()));
        return expanded;
    }

    private Set<String> terms(String value) {
        return Arrays.stream(normalize(value).split("[^a-z0-9]+"))
                .filter(term -> term.length() >= 3)
                .filter(term -> !STOP_WORDS.contains(term))
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }
}
