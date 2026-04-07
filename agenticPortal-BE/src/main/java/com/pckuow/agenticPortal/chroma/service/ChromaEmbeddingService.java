package com.pckuow.agenticPortal.chroma.service;

import com.pckuow.agenticPortal.core.service.OllamaEmbeddingService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChromaEmbeddingService {

    private final OllamaEmbeddingService ollamaEmbeddingService;
    private final EmbeddingStore<TextSegment> chromaEmbeddingStore;

    public ChromaEmbeddingService(
            OllamaEmbeddingService ollamaEmbeddingService,
            EmbeddingStore<TextSegment> chromaEmbeddingStore) {
        this.ollamaEmbeddingService = ollamaEmbeddingService;
        this.chromaEmbeddingStore = chromaEmbeddingStore;
    }

    public void add(String text, Map<String, Object> metadata) {

        Embedding embedding = ollamaEmbeddingService.embed(text);

        TextSegment segment = TextSegment.from(text, new Metadata(metadata));

        chromaEmbeddingStore.add(embedding, segment);
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
        log.info("Embedding match sopCode: {}", embeddingMatch.embedded().metadata().getString("sopCode"));

        return String.format("Found! SOP Code: %s", embeddingMatch.embedded().metadata().getString("sopCode"));
    }
}
