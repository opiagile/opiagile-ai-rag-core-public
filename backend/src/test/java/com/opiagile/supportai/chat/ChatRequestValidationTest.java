package com.opiagile.supportai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ChatRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deveExigirMensagem() {
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(new ChatRequest(null, "", "WEB", "demo", null));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Informe uma mensagem para o atendimento.");
    }

    @Test
    void deveLimitarTamanhoDaMensagem() {
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(new ChatRequest(null, "a".repeat(1201), "WEB", "demo", null));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("A mensagem deve ter no máximo 1200 caracteres.");
    }

    @Test
    void deveLimitarCanalEContato() {
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(new ChatRequest(
                null,
                "Olá",
                "W".repeat(33),
                "c".repeat(129),
                null));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains(
                        "O canal deve ter no máximo 32 caracteres.",
                        "O identificador de contato deve ter no máximo 128 caracteres.");
    }

    @Test
    void deveValidarIdiomaDaRespostaQuandoInformado() {
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(new ChatRequest(
                null,
                "Olá",
                "WEB",
                "demo",
                "FR"));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O idioma da resposta deve ser ENGLISH, SPANISH ou PORTUGUESE.");
    }
}
