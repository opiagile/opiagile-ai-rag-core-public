package com.opiagile.supportai.chat;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.tenant.TenantContextResolver;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final TenantContextResolver tenantContextResolver;

    public ChatController(ChatService chatService, TenantContextResolver tenantContextResolver) {
        this.chatService = chatService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public ChatResponse chat(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId,
            @Valid @RequestBody ChatRequest request) {
        return chatService.answer(tenantContextResolver.resolve(tenantId, workspaceId), request);
    }
}
