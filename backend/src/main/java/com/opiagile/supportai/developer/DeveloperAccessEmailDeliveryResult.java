package com.opiagile.supportai.developer;

record DeveloperAccessEmailDeliveryResult(
        boolean sent,
        String failureReason) {

    static DeveloperAccessEmailDeliveryResult success() {
        return new DeveloperAccessEmailDeliveryResult(true, null);
    }

    static DeveloperAccessEmailDeliveryResult failed(String failureReason) {
        return new DeveloperAccessEmailDeliveryResult(false, failureReason);
    }
}
