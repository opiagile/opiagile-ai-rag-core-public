package com.opiagile.supportai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSimilarityScorerTest {

    private final TextSimilarityScorer scorer = new TextSimilarityScorer();

    @Test
    void devePontuarConteudoComTermosRelacionados() {
        double score = scorer.score(
                "atendimento aos sábados",
                "A clínica atende aos sabados das 8h as 12h com horário agendado.");

        assertThat(score).isGreaterThanOrEqualTo(0.66);
    }

    @Test
    void deveRetornarZeroQuandoNaoHouverCorrespondencia() {
        double score = scorer.score("prazo de reembolso", "Documentos necessários para consulta médica.");

        assertThat(score).isZero();
    }

    @Test
    void deveGerarTrechoCurtoComInformacaoEncontrada() {
        String excerpt = scorer.excerpt(
                "remarcar consulta",
                "Consultas podem ser remarcadas com pelo menos 24 horas de antecedencia. O paciente deve informar o nome completo.",
                70);

        assertThat(excerpt).contains("Consultas");
        assertThat(excerpt.length()).isLessThanOrEqualTo(76);
    }

    @Test
    void devePontuarPerguntaEmInglesContraConteudoEmPortugues() {
        double score = scorer.score(
                "What information should I send?",
                "Para agilizar o atendimento, informe nome, contato, empresa ou departamento e um resumo objetivo da solicitação.");

        assertThat(score).isGreaterThanOrEqualTo(0.50);
    }

    @Test
    void devePontuarEspecialistaEmInglesContraConteudoEmPortugues() {
        double score = scorer.score(
                "My request needs a specialist.",
                "Quando a solicitação exige análise técnica, financeira ou comercial detalhada, a conversa deve ser encaminhada para uma pessoa responsável.");

        assertThat(score).isGreaterThanOrEqualTo(0.50);
    }
}
