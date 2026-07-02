package com.opiagile.supportai.developer;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opiagile.supportai.tenant.TemporarySandboxTenant;
import com.opiagile.supportai.tenant.TenantRepository;

@Service
public class TemporarySandboxCleanupService {

    private final TenantRepository tenantRepository;
    private final DeveloperAccessRequestRepository accessRequestRepository;
    private final int batchSize;

    public TemporarySandboxCleanupService(
            TenantRepository tenantRepository,
            DeveloperAccessRequestRepository accessRequestRepository,
            @Value("${developer-access.sandbox.cleanup.batch-size:25}") int batchSize) {
        this.tenantRepository = tenantRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.batchSize = Math.max(1, Math.min(batchSize, 100));
    }

    @Transactional
    public List<String> cleanupExpiredSandboxes() {
        return tenantRepository.findExpiredTemporarySandboxes(batchSize).stream()
                .filter(this::deleteSandbox)
                .map(TemporarySandboxTenant::tenantSlug)
                .toList();
    }

    private boolean deleteSandbox(TemporarySandboxTenant sandbox) {
        int deleted = tenantRepository.deleteTemporarySandbox(sandbox.tenantId());
        if (deleted == 1) {
            accessRequestRepository.markSandboxDeleted(sandbox.tenantSlug());
            return true;
        }
        return false;
    }
}
