package com.opiagile.supportai.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {

    private final int maxChars;
    private final int overlapChars;

    public DocumentChunker(
            @Value("${documents.chunk.max-chars:900}") int maxChars,
            @Value("${documents.chunk.overlap-chars:120}") int overlapChars) {
        if (maxChars < 100) {
            throw new IllegalArgumentException("documents.chunk.max-chars deve ser maior ou igual a 100");
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("documents.chunk.overlap-chars deve ser menor que max-chars");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<String> chunk(String rawContent) {
        String content = normalize(rawContent);
        if (content.isBlank()) {
            return List.of();
        }
        if (content.length() <= maxChars) {
            return List.of(content);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxChars, content.length());
            int adjustedEnd = adjustEnd(content, start, end);
            String chunk = content.substring(start, adjustedEnd).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (adjustedEnd >= content.length()) {
                break;
            }
            int nextStart = Math.max(0, adjustedEnd - overlapChars);
            start = adjustStart(content, nextStart, adjustedEnd);
        }
        return chunks;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private int adjustStart(String content, int nextStart, int previousEnd) {
        if (nextStart == 0 || !Character.isLetterOrDigit(content.charAt(nextStart))) {
            return nextStart;
        }
        if (!Character.isLetterOrDigit(content.charAt(nextStart - 1))) {
            return nextStart;
        }
        for (int index = nextStart; index < previousEnd; index++) {
            if (Character.isWhitespace(content.charAt(index))) {
                return index + 1;
            }
        }
        return nextStart;
    }

    private int adjustEnd(String content, int start, int end) {
        if (end >= content.length()) {
            return end;
        }
        int paragraphBreak = content.lastIndexOf("\n\n", end);
        if (paragraphBreak > start + maxChars / 2) {
            return paragraphBreak;
        }
        int sentenceBreak = Math.max(content.lastIndexOf(". ", end), content.lastIndexOf("\n", end));
        if (sentenceBreak > start + maxChars / 2) {
            return sentenceBreak + 1;
        }
        int space = content.lastIndexOf(' ', end);
        if (space > start + maxChars / 2) {
            return space;
        }
        return end;
    }
}
