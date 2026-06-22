package com.opiagile.supportai.handoff;

import jakarta.validation.constraints.NotBlank;

public record UpdateHandoffStatusRequest(
        @NotBlank(message = "Informe o novo status do handoff.") String status) {
}
