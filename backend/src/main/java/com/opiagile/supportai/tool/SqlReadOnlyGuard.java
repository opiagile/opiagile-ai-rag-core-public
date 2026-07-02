package com.opiagile.supportai.tool;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SqlReadOnlyGuard {

    private static final Pattern TABLE_PATTERN = Pattern.compile("(?i)\\b(?:from|join)\\s+([a-zA-Z_][\\w.]*|\"[^\"]+\")");
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|copy|call|execute|merge|refresh|vacuum|analyze|listen|notify|lock|set|reset|show)\\b");

    public String validate(String sql, ExternalToolRecord tool, int effectiveLimit) {
        String normalized = normalize(sql);
        if (!normalized.toLowerCase(Locale.ROOT).startsWith("select ")) {
            throw new IllegalArgumentException("A ferramenta SQL aceita apenas consultas SELECT simples.");
        }
        if (normalized.contains(";") || normalized.contains("--") || normalized.contains("/*") || normalized.contains("*/")) {
            throw new IllegalArgumentException("A consulta não pode conter múltiplos comandos ou comentários SQL.");
        }
        if (FORBIDDEN_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException("A consulta contém comando não permitido para modo somente leitura.");
        }
        Set<String> referencedTables = referencedTables(normalized);
        if (referencedTables.isEmpty()) {
            throw new IllegalArgumentException("A consulta deve referenciar pelo menos uma tabela permitida.");
        }
        Set<String> allowedTables = new LinkedHashSet<>(tool.allowedTables().stream()
                .map(this::normalizeTableName)
                .toList());
        for (String table : referencedTables) {
            if (!allowedTables.contains(table)) {
                throw new IllegalArgumentException("Tabela não permitida para esta ferramenta: " + table);
            }
        }
        return "SELECT * FROM (" + normalized + ") AS tool_query_result LIMIT " + effectiveLimit;
    }

    private Set<String> referencedTables(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        while (matcher.find()) {
            tables.add(normalizeTableName(matcher.group(1)));
        }
        return tables;
    }

    private String normalize(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.trim().replaceAll("\\s+", " ");
    }

    private String normalizeTableName(String table) {
        String normalized = table == null ? "" : table.trim().replace("\"", "");
        int dot = normalized.lastIndexOf('.');
        if (dot >= 0) {
            normalized = normalized.substring(dot + 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
