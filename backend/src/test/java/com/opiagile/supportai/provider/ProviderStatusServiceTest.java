package com.opiagile.supportai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.opiagile.supportai.chat.OpenAiChatModelProvider;
import com.opiagile.supportai.chat.RagAnswerPromptBuilder;
import com.opiagile.supportai.rag.EmbeddingProvider;

class ProviderStatusServiceTest {

    private final RagAnswerPromptBuilder promptBuilder = new RagAnswerPromptBuilder("profissional");

    @Test
    void deveExporModoDemoSemSegredos() {
        ProviderStatusService service = new ProviderStatusService(
                openAiProvider(""),
                embeddingProvider("noop"),
                "DEMO",
                "DEMO",
                false,
                "text-embedding-3-small",
                1536,
                true);

        ProviderStatusResponse response = service.status();

        assertThat(response.status()).isEqualTo("ATENCAO");
        assertThat(response.chat().activeProvider()).isEqualTo("DEMO");
        assertThat(response.chat().openAiApiKeyConfigured()).isFalse();
        assertThat(response.embeddings().activeProvider()).isEqualTo("NOOP");
        assertThat(response.embeddings().status()).isEqualTo("DESABILITADO_FALLBACK_TEXTUAL");
        assertThat(response.retrieval().activeStrategy()).isEqualTo("LOCAL_TEXT");
        assertThat(response.warnings()).contains("OPENAI_EMBEDDINGS_DESABILITADO; recuperação usa texto local até habilitar embeddings.");
        assertThat(response.toString()).doesNotContain("sk-");
    }

    @Test
    void deveIndicarFallbackQuandoOpenAiFoiSolicitadoSemChave() {
        ProviderStatusService service = new ProviderStatusService(
                openAiProvider(""),
                embeddingProvider("openai-sem-chave"),
                "LLM",
                "OPENAI",
                true,
                "text-embedding-3-small",
                1536,
                true);

        ProviderStatusResponse response = service.status();

        assertThat(response.status()).isEqualTo("ATENCAO");
        assertThat(response.chat().activeProvider()).isEqualTo("DEMO");
        assertThat(response.chat().status()).isEqualTo("FALLBACK_DEMO_POR_CHAVE_AUSENTE");
        assertThat(response.embeddings().status()).isEqualTo("FALLBACK_TEXTUAL_POR_CHAVE_AUSENTE");
        assertThat(response.retrieval().pgvectorReadyByConfiguration()).isFalse();
        assertThat(response.toolPlanner().status()).isEqualTo("INATIVO_POR_CHAVE_AUSENTE");
        assertThat(response.warnings()).contains(
                "OPENAI_CHAT_SOLICITADO_SEM_CHAVE; respostas usam fallback DEMO.",
                "OPENAI_EMBEDDINGS_HABILITADO_SEM_CHAVE; recuperação usa fallback textual.",
                "TOOLS_PLANNER_LLM_SEM_CHAVE; planner LLM fica inativo.");
    }

    @Test
    void deveIndicarPgvectorEOpenAiQuandoChaveConfigurada() {
        ProviderStatusService service = new ProviderStatusService(
                openAiProvider("openai-test-key"),
                embeddingProvider("openai"),
                "LLM",
                "OPENAI",
                true,
                "text-embedding-3-small",
                1536,
                true);

        ProviderStatusResponse response = service.status();

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.chat().activeProvider()).isEqualTo("OPENAI");
        assertThat(response.chat().model()).isEqualTo("gpt-5-mini");
        assertThat(response.chat().openAiApiKeyConfigured()).isTrue();
        assertThat(response.embeddings().activeProvider()).isEqualTo("OPENAI");
        assertThat(response.embeddings().status()).isEqualTo("OPERACIONAL");
        assertThat(response.retrieval().activeStrategy()).isEqualTo("PGVECTOR_COM_FALLBACK_TEXTUAL");
        assertThat(response.retrieval().pgvectorReadyByConfiguration()).isTrue();
        assertThat(response.toolPlanner().activeProvider()).isEqualTo("OPENAI");
        assertThat(response.warnings()).isEmpty();
        assertThat(response.toString()).doesNotContain("openai-test-key");
    }

    private OpenAiChatModelProvider openAiProvider(String apiKey) {
        return new OpenAiChatModelProvider(
                RestClient.builder(),
                promptBuilder,
                apiKey,
                "gpt-5-mini",
                600,
                30,
                "minimal",
                "low");
    }

    private EmbeddingProvider embeddingProvider(String name) {
        return new EmbeddingProvider() {
            @Override
            public Optional<float[]> embed(String text) {
                return Optional.empty();
            }

            @Override
            public String providerName() {
                return name;
            }
        };
    }
}
