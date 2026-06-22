package com.opiagile.supportai.lead;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class LeadExtractor {

    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("(\\+?55)?\\s?\\(?\\d{2}\\)?\\s?9?\\d{4}[-\\s]?\\d{4}");
    private static final Pattern NAME = Pattern.compile("(?i)(?:meu nome [ée]|me chamo|sou)\\s+([A-Za-zÀ-ÿ]+(?:\s+[A-Za-zÀ-ÿ]+){0,3})");

    public LeadExtraction extract(String message, Intent intent) {
        return new LeadExtraction(
                firstGroup(NAME, message),
                firstMatch(PHONE, message),
                firstMatch(EMAIL, message),
                inferInterest(message, intent));
    }

    private String inferInterest(String message, Intent intent) {
        if (intent == Intent.AGENDAR || intent == Intent.REMARCAR || intent == Intent.CANCELAR || intent == Intent.COMERCIAL) {
            return message;
        }
        return null;
    }

    private String firstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private String firstMatch(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group().trim();
    }
}
