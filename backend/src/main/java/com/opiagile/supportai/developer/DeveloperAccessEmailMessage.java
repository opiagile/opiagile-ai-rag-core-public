package com.opiagile.supportai.developer;

import java.util.UUID;

record DeveloperAccessEmailMessage(
        UUID id,
        UUID requestId,
        String emailType,
        String recipient,
        String subject,
        String htmlBody,
        String textBody) {
}
