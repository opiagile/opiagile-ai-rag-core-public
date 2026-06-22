package com.opiagile.supportai.handoff;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/handoffs")
public class HandoffController {

    private final HandoffService handoffService;

    public HandoffController(HandoffService handoffService) {
        this.handoffService = handoffService;
    }

    @GetMapping
    public List<HandoffResponse> findAll() {
        return handoffService.findAll();
    }

    @GetMapping("/{id}")
    public HandoffResponse findById(@PathVariable UUID id) {
        return handoffService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public HandoffResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateHandoffStatusRequest request) {
        return handoffService.updateStatus(id, request.status());
    }
}
