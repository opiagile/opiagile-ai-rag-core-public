package com.opiagile.supportai.developer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class DeveloperAccessNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeveloperAccessNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final DeveloperAccessEmailProperties properties;

    public DeveloperAccessNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            DeveloperAccessEmailProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    boolean enabled() {
        return properties.enabled();
    }

    DeveloperAccessEmailDeliveryResult send(DeveloperAccessEmailMessage email) {
        if (!properties.enabled()) {
            return DeveloperAccessEmailDeliveryResult.failed("EMAIL_DISABLED");
        }
        if (email.recipient() == null || email.recipient().isBlank()) {
            LOGGER.warn("Email developer ignorado: destinatário não configurado.");
            return DeveloperAccessEmailDeliveryResult.failed("EMAIL_RECIPIENT_NOT_CONFIGURED");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.warn("Email developer ignorado: JavaMailSender indisponível.");
            return DeveloperAccessEmailDeliveryResult.failed("MAIL_SENDER_UNAVAILABLE");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (!properties.from().isBlank()) {
                helper.setFrom(properties.from(), "Opiagile");
                helper.setReplyTo(properties.from(), "Opiagile");
            }
            helper.setTo(email.recipient());
            helper.setSubject(email.subject());
            helper.setText(email.textBody(), email.htmlBody());
            mailSender.send(message);
            return DeveloperAccessEmailDeliveryResult.success();
        } catch (Exception exception) {
            LOGGER.warn(
                    "Falha ao enviar email developer. requestId={}, emailType={}, motivo={}",
                    email.requestId(),
                    email.emailType(),
                    exception.getClass().getSimpleName());
            return DeveloperAccessEmailDeliveryResult.failed(exception.getClass().getSimpleName());
        }
    }
}
