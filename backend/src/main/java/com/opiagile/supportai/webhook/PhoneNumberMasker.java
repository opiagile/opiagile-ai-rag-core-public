package com.opiagile.supportai.webhook;

public final class PhoneNumberMasker {

    private PhoneNumberMasker() {
    }

    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }

    public static String mask(String phone) {
        String normalized = normalize(phone);
        if (normalized.length() <= 4) {
            return "****";
        }
        String prefix = normalized.substring(0, Math.min(4, normalized.length()));
        String suffix = normalized.substring(Math.max(prefix.length(), normalized.length() - 4));
        return prefix + "****" + suffix;
    }
}
