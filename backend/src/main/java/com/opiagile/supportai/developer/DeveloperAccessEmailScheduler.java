package com.opiagile.supportai.developer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeveloperAccessEmailScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeveloperAccessEmailScheduler.class);

    private final DeveloperAccessRequestRepository repository;
    private final DeveloperAccessEmailOutboxRepository outboxRepository;
    private final DeveloperAccessNotificationService notificationService;
    private final boolean schedulerEnabled;
    private final int batchSize;
    private final int maxAttempts;
    private final int retryDelayMinutes;

    public DeveloperAccessEmailScheduler(
            DeveloperAccessRequestRepository repository,
            DeveloperAccessEmailOutboxRepository outboxRepository,
            DeveloperAccessNotificationService notificationService,
            @Value("${developer-access.email.scheduler.enabled:true}") boolean schedulerEnabled,
            @Value("${developer-access.email.scheduler.batch-size:5}") int batchSize,
            @Value("${developer-access.email.scheduler.max-attempts:10}") int maxAttempts,
            @Value("${developer-access.email.scheduler.retry-delay-minutes:15}") int retryDelayMinutes) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.notificationService = notificationService;
        this.schedulerEnabled = schedulerEnabled;
        this.batchSize = Math.max(1, batchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelayMinutes = Math.max(1, retryDelayMinutes);
    }

    @Scheduled(fixedDelayString = "${developer-access.email.scheduler.fixed-delay-ms:60000}")
    public void processPendingNotifications() {
        if (!schedulerEnabled || !notificationService.enabled()) {
            return;
        }

        List<DeveloperAccessEmailMessage> pending = outboxRepository.findPending(batchSize, maxAttempts);
        for (DeveloperAccessEmailMessage email : pending) {
            DeveloperAccessEmailDeliveryResult result = notificationService.send(email);
            if (result.sent()) {
                outboxRepository.markSent(email.id());
                if (DeveloperAccessEmailType.ADMIN_NEW_REQUEST.equals(email.emailType())) {
                    repository.markEmailSent(email.requestId());
                }
            } else {
                outboxRepository.markFailed(email.id(), result.failureReason(), retryDelayMinutes);
                if (DeveloperAccessEmailType.ADMIN_NEW_REQUEST.equals(email.emailType())) {
                    repository.markEmailFailed(email.requestId(), result.failureReason(), retryDelayMinutes);
                }
                LOGGER.warn(
                        "Email developer permanece pendente para retry. requestId={}, emailType={}, motivo={}",
                        email.requestId(),
                        email.emailType(),
                        result.failureReason());
            }
        }
    }
}
