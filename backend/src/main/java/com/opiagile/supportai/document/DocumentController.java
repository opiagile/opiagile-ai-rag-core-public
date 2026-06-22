package com.opiagile.supportai.document;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return documentService.upload(file);
    }

    @GetMapping
    public List<DocumentSummaryResponse> findAll() {
        return documentService.findAll();
    }

    @GetMapping("/{id}")
    public DocumentDetailResponse findById(@PathVariable UUID id) {
        return documentService.findById(id);
    }

    @GetMapping("/{id}/chunks")
    public List<DocumentChunkResponse> findChunks(@PathVariable UUID id) {
        return documentService.findChunks(id);
    }
}
