package com.opiagile.supportai.rag;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagRetrievalService {

    private final RagChunkRepository chunkRepository;
    private final TextSimilarityScorer scorer;
    private final EmbeddingProvider embeddingProvider;
    private final int topK;
    private final double minScore;

    public RagRetrievalService(
            RagChunkRepository chunkRepository,
            TextSimilarityScorer scorer,
            EmbeddingProvider embeddingProvider,
            @Value("${rag.top-k:5}") int topK,
            @Value("${rag.min-score:0.15}") double minScore) {
        this.chunkRepository = chunkRepository;
        this.scorer = scorer;
        this.embeddingProvider = embeddingProvider;
        this.topK = Math.max(1, topK);
        this.minScore = Math.max(0.0, minScore);
    }

    public List<RetrievedChunk> retrieve(String query) {
        Optional<float[]> queryEmbedding = embeddingProvider.embed(query);
        if (queryEmbedding.isPresent()) {
            List<RetrievedChunk> vectorChunks = retrieveByVector(queryEmbedding.get());
            if (!vectorChunks.isEmpty()) {
                return vectorChunks;
            }
        }
        return retrieveByText(query);
    }

    private List<RetrievedChunk> retrieveByVector(float[] queryEmbedding) {
        return chunkRepository.findNearestIndexedChunks(queryEmbedding, topK).stream()
                .filter(chunk -> chunk.score() != null && chunk.score() >= minScore)
                .map(chunk -> toVectorRetrievedChunk(chunk))
                .toList();
    }

    private List<RetrievedChunk> retrieveByText(String query) {
        return chunkRepository.findAllIndexedChunks().stream()
                .map(chunk -> toTextRetrievedChunk(query, chunk))
                .filter(chunk -> chunk.score() >= minScore)
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private RetrievedChunk toTextRetrievedChunk(String query, StoredChunk chunk) {
        double score = scorer.score(query, chunk.content());
        return new RetrievedChunk(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.filename(),
                chunk.content(),
                score,
                scorer.excerpt(query, chunk.content(), 320),
                "local-text");
    }

    private RetrievedChunk toVectorRetrievedChunk(StoredChunk chunk) {
        return new RetrievedChunk(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.filename(),
                chunk.content(),
                chunk.score(),
                excerpt(chunk.content(), 320),
                "pgvector-" + embeddingProvider.providerName());
    }

    private String excerpt(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength).trim() + "...";
    }
}
