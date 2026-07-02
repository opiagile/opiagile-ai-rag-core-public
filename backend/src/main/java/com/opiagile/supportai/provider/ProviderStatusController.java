package com.opiagile.supportai.provider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "provider-controller", description = "Status seguro de provedores IA e fallback.")
public class ProviderStatusController {

    private final ProviderStatusService service;

    public ProviderStatusController(ProviderStatusService service) {
        this.service = service;
    }

    @GetMapping("/status")
    @Operation(summary = "Consulta status seguro de LLM, embeddings, recuperação e planner")
    public ProviderStatusResponse status() {
        return service.status();
    }
}
