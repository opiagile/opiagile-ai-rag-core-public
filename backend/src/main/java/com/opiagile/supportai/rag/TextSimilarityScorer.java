package com.opiagile.supportai.rag;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class TextSimilarityScorer {

    private static final Set<String> STOP_WORDS = Set.of(
            "para", "com", "uma", "como", "que", "por", "das", "dos", "nas", "nos",
            "qual", "quais", "quando", "onde", "sobre", "voce", "voces", "isso", "esta");

    public double score(String query, String content) {
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty() || content == null || content.isBlank()) {
            return 0.0;
        }
        Set<String> contentTerms = terms(content);
        long matches = queryTerms.stream()
                .filter(contentTerms::contains)
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
                .map(normalizedContent::indexOf)
                .filter(index -> index >= 0)
                .min(Integer::compareTo)
                .orElse(-1);
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
