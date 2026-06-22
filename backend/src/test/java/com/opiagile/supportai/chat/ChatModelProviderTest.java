package com.opiagile.supportai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.opiagile.supportai.conversation.MessageRecord;
import com.opiagile.supportai.lead.Intent;
import com.opiagile.supportai.rag.RetrievedChunk;

class ChatModelProviderTest {

    private final RagAnswerPromptBuilder promptBuilder = new RagAnswerPromptBuilder("profissional e acolhedor");
    private final DemoChatModelProvider demoProvider = new DemoChatModelProvider();

    @Test
    void deveUsarFallbackDemoQuandoOpenAiFoiSolicitadoSemChave() {
        OpenAiChatModelProvider openAiProvider = new OpenAiChatModelProvider(
                RestClient.builder(),
                promptBuilder,
                "",
                "gpt-5-mini",
                600,
                30,
                "minimal",
                "low");
        RoutingChatModelProvider provider = new RoutingChatModelProvider(
                demoProvider,
                openAiProvider,
                "LLM",
                "OPENAI");

        ChatGenerationResult result = provider.generate(prompt());

        assertThat(result.responseMode()).isEqualTo("DEMO");
        assertThat(result.llmProvider()).isEqualTo("DEMO");
        assertThat(result.fallbackReason()).isEqualTo("OPENAI_API_KEY_AUSENTE");
        assertThat(result.answer()).contains("informação relevante");
    }

    @Test
    void deveManterModoDemoQuandoConfiguradoSemLlm() {
        OpenAiChatModelProvider openAiProvider = new OpenAiChatModelProvider(
                RestClient.builder(),
                promptBuilder,
                "",
                "gpt-5-mini",
                600,
                30,
                "minimal",
                "low");
        RoutingChatModelProvider provider = new RoutingChatModelProvider(
                demoProvider,
                openAiProvider,
                "DEMO",
                "DEMO");

        ChatGenerationResult result = provider.generate(prompt());

        assertThat(result.responseMode()).isEqualTo("DEMO");
        assertThat(result.fallbackReason()).isNull();
    }

    @Test
    void promptDeAtendimentoDeveIncluirHistoricoFontesERegrasDeSeguranca() {
        ChatPrompt prompt = prompt();

        String instructions = promptBuilder.instructions();
        String input = promptBuilder.input(prompt);

        assertThat(instructions).contains("Não invente informações");
        assertThat(instructions).contains("não se apresente como pessoa humana");
        assertThat(instructions).contains("não faça nova pergunta");
        assertThat(input).contains("Histórico recente");
        assertThat(input).contains("USER: Meu nome é João");
        assertThat(input).contains("faq.txt");
        assertThat(input.toLowerCase()).contains("atendemos aos sábados");
    }

    private ChatPrompt prompt() {
        UUID conversationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        return new ChatPrompt(
                "Vocês atendem aos sábados?",
                Intent.DUVIDA_FAQ,
                "QUALIFYING",
                false,
                null,
                List.of(new MessageRecord(
                        UUID.randomUUID(),
                        conversationId,
                        "USER",
                        "Meu nome é João",
                        Intent.COMERCIAL.name(),
                        OffsetDateTime.now())),
                List.of(new RetrievedChunk(
                        chunkId,
                        documentId,
                        "faq.txt",
                        "Atendemos aos sábados das 8h às 12h.",
                        0.82,
                        "Atendemos aos sábados das 8h às 12h.",
                        "local-text")));
    }
}
