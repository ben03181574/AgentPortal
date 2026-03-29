package com.tsmc.agenticPortal.agent.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OllamaEmbeddingService {

    private final OllamaEmbeddingModel ollamaEmbeddingModel;

    public OllamaEmbeddingService(OllamaEmbeddingModel ollamaEmbeddingModel) {
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
    }

    public Embedding embed(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }

        Embedding embedding = ollamaEmbeddingModel.embed(trimmed).content();

        log.info("Embedding {} has been embedded, size: {}", trimmed, embedding.vector().length);
        return embedding;
    }
}