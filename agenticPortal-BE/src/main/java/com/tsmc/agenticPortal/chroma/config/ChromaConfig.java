package com.tsmc.agenticPortal.chroma.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static dev.langchain4j.store.embedding.chroma.ChromaApiVersion.V2;

public class ChromaConfig  {

    @Value("${chroma.base-url}")
    private String baseUrl;

    @Value("${chroma.collection-name}")
    private String collectionName;

    @Bean
    public EmbeddingStore<TextSegment> chromaEmbeddingStore(){
        return ChromaEmbeddingStore.builder()
                .apiVersion(V2)
                .baseUrl(baseUrl)
                .collectionName(collectionName)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
