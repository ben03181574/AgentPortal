package com.tsmc.agenticPortal.chroma.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbeddingSearchService {

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> chromaEmbeddingStore;

    public String search(String text) {

        Embedding queryEmbedding = embeddingModel.embed(text).content();
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
