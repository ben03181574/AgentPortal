package com.tsmc.agenticPortal.chroma.service;

import com.tsmc.agenticPortal.agent.service.OllamaEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    private final OllamaEmbeddingService ollamaEmbeddingService;
    private final EmbeddingStore<TextSegment> chromaEmbeddingStore;

    public EmbeddingService(OllamaEmbeddingService ollamaEmbeddingService, EmbeddingStore<TextSegment> chromaEmbeddingStore) {
        this.ollamaEmbeddingService = ollamaEmbeddingService;
        this.chromaEmbeddingStore = chromaEmbeddingStore;
    }

    public void add(String text) {
        Embedding embedding = ollamaEmbeddingService.embed(text);
        chromaEmbeddingStore.add(embedding, TextSegment.from(text));
    }

    public String search(String text) {

        Embedding queryEmbedding = ollamaEmbeddingService.embed(text);

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = chromaEmbeddingStore.search(embeddingSearchRequest).matches();

        if (matches.isEmpty()) {
            return "No match";
        }

        EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);

        log.info("Embedding match score: {}", embeddingMatch.score());
        log.info("Embedding match text: {}", embeddingMatch.embedded().text());

        return "Found!! Doc content: " + embeddingMatch.embedded().text();

    }
}
