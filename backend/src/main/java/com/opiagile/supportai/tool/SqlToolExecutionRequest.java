package com.opiagile.supportai.tool;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SqlToolExecutionRequest(
        @NotBlank(message = "Informe a consulta SQL.")
        @Size(max = 4000, message = "A consulta SQL deve ter no máximo 4000 caracteres.")
        String sql,

        @Min(value = 1, message = "maxRows deve ser maior ou igual a 1.")
        @Max(value = 100, message = "maxRows deve ser menor ou igual a 100.")
        Integer maxRows) {
}
