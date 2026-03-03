package com.tsmc.agenticPortal.agent.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class OllamaEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public OllamaEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = requireNonNull(embeddingModel, "embeddingModel must not be null");
    }

    public Embedding embed(String text) {
        String cleaned = normalize(text);
        Embedding embedding = embeddingModel.embed(cleaned).content();

        log.info("Embedding {} has been embedded, size: {}", cleaned, embedding.vector().length);
        return embedding;
    }

    public List<Embedding> embedAll(List<String> texts) {
        requireNonNull(texts, "texts must not be null");

        List<TextSegment> segments = new ArrayList<>(texts.size());
        for (String t : texts) {
            String cleaned = normalize(t);
            segments.add(TextSegment.from(cleaned));
        }

        return embeddingModel.embedAll(segments).content();
    }

    private String normalize(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }
        return trimmed;
    }
}