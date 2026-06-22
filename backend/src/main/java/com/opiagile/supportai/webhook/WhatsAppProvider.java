package com.opiagile.supportai.webhook;

public interface WhatsAppProvider {

    String providerName();

    void sendMessage(String to, String message);
}
