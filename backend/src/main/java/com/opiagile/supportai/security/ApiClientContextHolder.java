package com.opiagile.supportai.security;

import java.util.Optional;

public final class ApiClientContextHolder {

    private static final ThreadLocal<ApiClientContext> CURRENT = new ThreadLocal<>();

    private ApiClientContextHolder() {
    }

    public static void set(ApiClientContext context) {
        CURRENT.set(context);
    }

    public static Optional<ApiClientContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
