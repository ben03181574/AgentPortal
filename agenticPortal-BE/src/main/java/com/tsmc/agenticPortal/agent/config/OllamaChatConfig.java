package com.tsmc.agenticPortal.agent.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OllamaChatConfig {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.chat-model-name}")
    private String modelName;

    @Bean
    public StreamingChatModel streamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .timeout(java.time.Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .customHeaders(Map.of("Content-Type", "application/json;charset=UTF-8"))
                .build();
    }
}
