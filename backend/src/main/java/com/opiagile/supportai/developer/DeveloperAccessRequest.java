package com.opiagile.supportai.developer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeveloperAccessRequest(
        @NotBlank(message = "Informe seu nome.")
        @Size(max = 160, message = "O nome deve ter no máximo 160 caracteres.")
        String name,

        @Size(max = 180, message = "A empresa deve ter no máximo 180 caracteres.")
        String company,

        @NotBlank(message = "Informe um email de contato.")
        @Email(message = "Informe um email válido.")
        @Size(max = 180, message = "O email deve ter no máximo 180 caracteres.")
        String email,

        @NotBlank(message = "Descreva o objetivo do teste.")
        @Size(max = 2000, message = "O objetivo do teste deve ter no máximo 2000 caracteres.")
        String useCase,

        @Size(max = 1000, message = "Os recursos pretendidos devem ter no máximo 1000 caracteres.")
        String requestedResources,

        @Size(max = 120, message = "Origem inválida.")
        String website) {
}
