package com.opiagile.supportai.webhook;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private String provider = "MOCK";
    private String verifyToken = "";
    private String appSecret = "";
    private String accessToken = "";
    private String phoneNumberId = "";
    private String businessAccountId = "";
    private String graphApiVersion = "v23.0";
    private List<String> allowedTestNumbers = new ArrayList<>();
    private String publicBaseUrl = "";
    private boolean sendEnabled = false;
    private boolean dryRun = true;
    private boolean signatureRequired = true;
    private int rateLimitPerMinute = 5;
    private boolean blockUnauthorized = true;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getVerifyToken() { return verifyToken; }
    public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
    public String getBusinessAccountId() { return businessAccountId; }
    public void setBusinessAccountId(String businessAccountId) { this.businessAccountId = businessAccountId; }
    public String getGraphApiVersion() { return graphApiVersion; }
    public void setGraphApiVersion(String graphApiVersion) { this.graphApiVersion = graphApiVersion; }
    public List<String> getAllowedTestNumbers() { return allowedTestNumbers; }
    public void setAllowedTestNumbers(List<String> allowedTestNumbers) { this.allowedTestNumbers = allowedTestNumbers == null ? new ArrayList<>() : allowedTestNumbers; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public boolean isSendEnabled() { return sendEnabled; }
    public void setSendEnabled(boolean sendEnabled) { this.sendEnabled = sendEnabled; }
    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
    public boolean isSignatureRequired() { return signatureRequired; }
    public void setSignatureRequired(boolean signatureRequired) { this.signatureRequired = signatureRequired; }
    public int getRateLimitPerMinute() { return Math.max(1, rateLimitPerMinute); }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }
    public boolean isBlockUnauthorized() { return blockUnauthorized; }
    public void setBlockUnauthorized(boolean blockUnauthorized) { this.blockUnauthorized = blockUnauthorized; }

    public boolean isMetaCloud() {
        return "META_CLOUD".equalsIgnoreCase(provider) || "OFFICIAL_CLOUD_API".equalsIgnoreCase(provider);
    }

    public boolean hasAccessToken() { return accessToken != null && !accessToken.isBlank(); }
    public boolean hasAppSecret() { return appSecret != null && !appSecret.isBlank(); }
    public boolean hasVerifyToken() { return verifyToken != null && !verifyToken.isBlank(); }
    public boolean hasPhoneNumberId() { return phoneNumberId != null && !phoneNumberId.isBlank(); }
    public boolean hasPublicBaseUrl() { return publicBaseUrl != null && !publicBaseUrl.isBlank(); }
}
