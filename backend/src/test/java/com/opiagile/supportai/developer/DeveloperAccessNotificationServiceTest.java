package com.opiagile.supportai.developer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.internet.MimeMessage;

class DeveloperAccessNotificationServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> mailSenderProvider = org.mockito.Mockito.mock(ObjectProvider.class);
    private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);

    @Test
    void deveEnviarEmailQuandoConfigurado() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        DeveloperAccessNotificationService service = new DeveloperAccessNotificationService(
                mailSenderProvider,
                new DeveloperAccessEmailProperties(
                        true,
                        "responsavel@opiagile.com",
                        "contato@opiagile.com",
                        "[Opiagile] Sandbox",
                        "https://opiagile.com",
                        24));

        DeveloperAccessEmailMessage email = new DeveloperAccessEmailMessage(
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                DeveloperAccessEmailType.LEAD_SANDBOX_APPROVED,
                "maria@example.com",
                "Assunto comercial",
                "<p>Mensagem <strong>HTML</strong></p>",
                "Mensagem texto");

        DeveloperAccessEmailDeliveryResult result = service.send(email);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(result.sent()).isTrue();
        MimeMessage message = messageCaptor.getValue();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("maria@example.com");
        assertThat(message.getFrom()[0].toString()).contains("contato@opiagile.com");
        assertThat(message.getSubject()).isEqualTo("Assunto comercial");
    }

    @Test
    void naoEnviaQuandoDesabilitado() {
        DeveloperAccessNotificationService service = new DeveloperAccessNotificationService(
                mailSenderProvider,
                new DeveloperAccessEmailProperties(false, "contato@opiagile.com", "", "[Opiagile] Sandbox", "https://opiagile.com", 24));

        DeveloperAccessEmailDeliveryResult result = service.send(
                new DeveloperAccessEmailMessage(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        DeveloperAccessEmailType.LEAD_REQUEST_CONFIRMATION,
                        "maria@example.com",
                        "Assunto",
                        "<p>HTML</p>",
                        "Texto"));

        assertThat(result.sent()).isFalse();
        assertThat(result.failureReason()).isEqualTo("EMAIL_DISABLED");
        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void naoFalhaQuandoMailSenderNaoExiste() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        DeveloperAccessNotificationService service = new DeveloperAccessNotificationService(
                mailSenderProvider,
                new DeveloperAccessEmailProperties(true, "contato@opiagile.com", "", "[Opiagile] Sandbox", "https://opiagile.com", 24));

        DeveloperAccessEmailDeliveryResult result = service.send(
                new DeveloperAccessEmailMessage(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        DeveloperAccessEmailType.LEAD_REQUEST_CONFIRMATION,
                        "maria@example.com",
                        "Assunto",
                        "<p>HTML</p>",
                        "Texto"));

        assertThat(result.sent()).isFalse();
        assertThat(result.failureReason()).isEqualTo("MAIL_SENDER_UNAVAILABLE");
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }
}
