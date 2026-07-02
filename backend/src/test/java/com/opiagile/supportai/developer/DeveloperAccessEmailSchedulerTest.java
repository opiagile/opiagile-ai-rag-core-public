package com.opiagile.supportai.developer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DeveloperAccessEmailSchedulerTest {

    private final DeveloperAccessRequestRepository repository =
            org.mockito.Mockito.mock(DeveloperAccessRequestRepository.class);
    private final DeveloperAccessEmailOutboxRepository outboxRepository =
            org.mockito.Mockito.mock(DeveloperAccessEmailOutboxRepository.class);
    private final DeveloperAccessNotificationService notificationService =
            org.mockito.Mockito.mock(DeveloperAccessNotificationService.class);

    @Test
    void deveMarcarComoEnviadoQuandoEmailForEntregue() {
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        DeveloperAccessEmailMessage email = email(requestId, DeveloperAccessEmailType.ADMIN_NEW_REQUEST);
        when(notificationService.enabled()).thenReturn(true);
        when(outboxRepository.findPending(5, 10)).thenReturn(List.of(email));
        when(notificationService.send(email)).thenReturn(DeveloperAccessEmailDeliveryResult.success());

        DeveloperAccessEmailScheduler scheduler = new DeveloperAccessEmailScheduler(
                repository, outboxRepository, notificationService, true, 5, 10, 15);

        scheduler.processPendingNotifications();

        verify(notificationService).send(email);
        verify(outboxRepository).markSent(email.id());
        verify(repository).markEmailSent(requestId);
        verify(repository, never()).markEmailFailed(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.anyInt());
    }

    @Test
    void deveManterPendenteQuandoEnvioFalhar() {
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000124");
        DeveloperAccessEmailMessage email = email(requestId, DeveloperAccessEmailType.ADMIN_NEW_REQUEST);
        when(notificationService.enabled()).thenReturn(true);
        when(outboxRepository.findPending(5, 10)).thenReturn(List.of(email));
        when(notificationService.send(email)).thenReturn(DeveloperAccessEmailDeliveryResult.failed("MailAuthenticationException"));

        DeveloperAccessEmailScheduler scheduler = new DeveloperAccessEmailScheduler(
                repository, outboxRepository, notificationService, true, 5, 10, 15);

        scheduler.processPendingNotifications();

        verify(outboxRepository).markFailed(email.id(), "MailAuthenticationException", 15);
        verify(repository).markEmailFailed(requestId, "MailAuthenticationException", 15);
        verify(repository, never()).markEmailSent(requestId);
    }

    @Test
    void naoAtualizaStatusLegadoParaEmailDoLead() {
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000125");
        DeveloperAccessEmailMessage email = email(requestId, DeveloperAccessEmailType.LEAD_REQUEST_CONFIRMATION);
        when(notificationService.enabled()).thenReturn(true);
        when(outboxRepository.findPending(5, 10)).thenReturn(List.of(email));
        when(notificationService.send(email)).thenReturn(DeveloperAccessEmailDeliveryResult.success());

        DeveloperAccessEmailScheduler scheduler = new DeveloperAccessEmailScheduler(
                repository, outboxRepository, notificationService, true, 5, 10, 15);

        scheduler.processPendingNotifications();

        verify(outboxRepository).markSent(email.id());
        verify(repository, never()).markEmailSent(requestId);
    }

    @Test
    void naoProcessaQuandoSchedulerEstiverDesabilitado() {
        DeveloperAccessEmailScheduler scheduler = new DeveloperAccessEmailScheduler(
                repository, outboxRepository, notificationService, false, 5, 10, 15);

        scheduler.processPendingNotifications();

        verify(outboxRepository, never()).findPending(org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyInt());
        verify(notificationService, never()).send(org.mockito.Mockito.any());
    }

    @Test
    void naoConsomeTentativasQuandoEmailEstiverDesabilitado() {
        when(notificationService.enabled()).thenReturn(false);
        DeveloperAccessEmailScheduler scheduler = new DeveloperAccessEmailScheduler(
                repository, outboxRepository, notificationService, true, 5, 10, 15);

        scheduler.processPendingNotifications();

        verify(outboxRepository, never()).findPending(org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyInt());
        verify(notificationService, never()).send(org.mockito.Mockito.any());
    }

    private DeveloperAccessEmailMessage email(UUID requestId, String emailType) {
        return new DeveloperAccessEmailMessage(
                UUID.randomUUID(),
                requestId,
                emailType,
                "maria@example.com",
                "Assunto",
                "<p>HTML</p>",
                "Texto");
    }

    @SuppressWarnings("unused")
    private DeveloperAccessNotificationRequest request(UUID requestId) {
        return new DeveloperAccessNotificationRequest(
                requestId,
                OffsetDateTime.parse("2026-06-27T12:00:00Z"),
                "Maria Silva",
                "Empresa Demo",
                "maria@example.com",
                "Validar integração.",
                "chat e documentos");
    }
}
