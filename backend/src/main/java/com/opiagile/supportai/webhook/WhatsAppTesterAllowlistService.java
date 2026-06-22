package com.opiagile.supportai.webhook;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppTesterAllowlistService {

    private final WhatsAppProperties properties;

    public WhatsAppTesterAllowlistService(WhatsAppProperties properties) {
        this.properties = properties;
    }

    public boolean isAllowed(String phone) {
        Set<String> allowed = normalizedAllowedNumbers();
        return !allowed.isEmpty() && allowed.contains(PhoneNumberMasker.normalize(phone));
    }

    public boolean hasAllowedNumbers() {
        return !normalizedAllowedNumbers().isEmpty();
    }

    public int allowedCount() {
        return normalizedAllowedNumbers().size();
    }

    private Set<String> normalizedAllowedNumbers() {
        return properties.getAllowedTestNumbers().stream()
                .map(PhoneNumberMasker::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }
}
