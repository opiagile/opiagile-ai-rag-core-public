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
import com.opiagile.supportai.tool.ExternalToolRecord;

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
        assertThat(result.answer()).contains("Pela base de conhecimento deste workspace");
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
    void modoDemoDeveSolicitarDadosDeContatoQuandoUsuarioPedeHumano() {
        ChatGenerationResult result = demoProvider.generate(promptComPedidoHumano());

        assertThat(result.answer()).contains("nome");
        assertThat(result.answer()).contains("empresa");
        assertThat(result.answer()).contains("email ou telefone");
        assertThat(result.answer()).contains("resumo");
    }

    @Test
    void promptDeAtendimentoDeveIncluirHistoricoFontesERegrasDeSeguranca() {
        ChatPrompt prompt = prompt();

        String instructions = promptBuilder.instructions();
        String input = promptBuilder.input(prompt);

        assertThat(instructions).contains("Não invente informações");
        assertThat(instructions).contains("Não use conhecimento geral do modelo");
        assertThat(instructions).contains("matemática");
        assertThat(instructions).contains("não se apresente como pessoa humana");
        assertThat(instructions).contains("não faça nova pergunta");
        assertThat(instructions).contains("https://demo-rag.opiagile.com");
        assertThat(instructions).contains("não repita pedido de contato");
        assertThat(input).contains("Histórico recente");
        assertThat(input).contains("não responda com conhecimento geral");
        assertThat(input).contains("USER: Meu nome é João");
        assertThat(input).contains("faq.txt");
        assertThat(input.toLowerCase()).contains("atendemos aos sábados");
        assertThat(input).contains("Base de conhecimento somente leitura");
    }

    @Test
    void modoDemoDeveApontarParaDemoOnlineSemPedirContatoQuandoUsuarioQuerTestar() {
        ChatGenerationResult result = demoProvider.generate(promptComPedidoDeTeste());

        assertThat(result.answer()).contains("https://demo-rag.opiagile.com");
        assertThat(result.answer()).contains("subir um documento");
        assertThat(result.answer()).doesNotContain("email");
        assertThat(result.answer()).doesNotContain("telefone");
    }

    @Test
    void modoDemoDeveResponderIntegracaoComApiEBancoSemExporTrechoBruto() {
        ChatGenerationResult result = demoProvider.generate(promptComIntegracaoBanco());

        assertThat(result.answer()).contains("integrada com chat web, WhatsApp, CRM, sistemas internos, APIs e bancos de dados");
        assertThat(result.answer()).contains("credenciais seguras");
        assertThat(result.answer()).doesNotContain("...");
        assertThat(result.answer()).doesNotContain("Pela base da Opiagile");
    }

    private ChatPrompt prompt() {
        UUID conversationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        return new ChatPrompt(
                "Vocês atendem aos sábados?",
                Intent.DUVIDA_FAQ,
                "QUALIFYING",
                "PORTUGUESE",
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
                        "local-text")),
                tools(),
                List.of());
    }

    private ChatPrompt promptComPedidoHumano() {
        return new ChatPrompt(
                "Quero falar com uma pessoa",
                Intent.FALAR_COM_HUMANO,
                "NEEDS_HUMAN",
                "PORTUGUESE",
                true,
                "Usuário pediu atendimento humano.",
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private ChatPrompt promptComPedidoDeTeste() {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        return new ChatPrompt(
                "Tem como eu testar online?",
                Intent.DUVIDA_FAQ,
                "QUALIFYING",
                "PORTUGUESE",
                false,
                null,
                List.of(),
                List.of(new RetrievedChunk(
                        chunkId,
                        documentId,
                        "opiagile-rag-conhecimento.md",
                        "A demo RAG da Opiagile fica em demo-rag.opiagile.com.",
                        0.9,
                        "A demo RAG da Opiagile fica em demo-rag.opiagile.com.",
                        "local-text")),
                tools(),
                List.of());
    }

    private ChatPrompt promptComIntegracaoBanco() {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        return new ChatPrompt(
                "Consigo integrar com minha aplicação e buscar informações do meu banco de dados?",
                Intent.DESCONHECIDO,
                "QUALIFYING",
                "PORTUGUESE",
                false,
                null,
                List.of(),
                List.of(new RetrievedChunk(
                        chunkId,
                        documentId,
                        "opiagile-rag-conhecimento.md",
                        "A Opiagile pode ser adaptada para consultar bases de dados e sistemas internos quando o caso de uso exigir.",
                        0.9,
                        "...A Opiagile pode ser adaptada para consultar bases de dados e sistemas internos quando o caso de uso exigir.",
                        "local-text")),
                tools(),
                List.of());
    }

    private List<ExternalToolRecord> tools() {
        return List.of(new ExternalToolRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "base-conhecimento-readonly",
                "Base de conhecimento somente leitura",
                "SQL_READ_ONLY",
                "ACTIVE",
                "Permite consultar metadados da base do workspace com controle de acesso.",
                List.of("documents", "document_chunks", "retrieval_logs"),
                20,
                50));
    }
}
