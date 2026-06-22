package com.opiagile.supportai.document;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tenant.TenantContextResolver;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final TenantContextResolver tenantContextResolver;

    public DocumentController(DocumentService documentService, TenantContextResolver tenantContextResolver) {
        this.documentService = documentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId,
            @RequestPart("file") MultipartFile file) {
        return documentService.upload(resolve(tenantId, workspaceId), file);
    }

    @GetMapping
    public List<DocumentSummaryResponse> findAll(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId) {
        return documentService.findAll(resolve(tenantId, workspaceId));
    }

    @GetMapping("/{id}")
    public DocumentDetailResponse findById(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId,
            @PathVariable UUID id) {
        return documentService.findById(resolve(tenantId, workspaceId), id);
    }

    @GetMapping("/{id}/chunks")
    public List<DocumentChunkResponse> findChunks(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId,
            @PathVariable UUID id) {
        return documentService.findChunks(resolve(tenantId, workspaceId), id);
    }

    private TenantContext resolve(String tenantId, String workspaceId) {
        return tenantContextResolver.resolve(tenantId, workspaceId);
    }
}
