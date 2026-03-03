package com.tsmc.agenticPortal.chroma.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbeddingSearchService {

    private final OllamaEmbeddingModel ollamaEmbeddingModel;
    private final EmbeddingStore<TextSegment> chromaEmbeddingStore;

    public EmbeddingSearchService(OllamaEmbeddingModel ollamaEmbeddingModel, EmbeddingStore<TextSegment> chromaEmbeddingStore) {
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
        this.chromaEmbeddingStore = chromaEmbeddingStore;
    }

    public String search(String text) {

        Embedding queryEmbedding = ollamaEmbeddingModel.embed(text).content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = chromaEmbeddingStore.search(embeddingSearchRequest).matches();
        EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);

        log.info("Embedding match score: {}", embeddingMatch.score());
        log.info("Embedding match text: {}", embeddingMatch.embedded().text());

        return embeddingMatch.embedded().text();
    }
}
