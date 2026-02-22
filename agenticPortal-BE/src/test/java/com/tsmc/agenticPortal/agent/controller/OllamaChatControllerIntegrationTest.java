package com.tsmc.agenticPortal.agent.controller;

import com.tsmc.agenticPortal.agent.dto.OllamaChatRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class OllamaChatControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testChatEndpoint_validationError_emptyMemoryId() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setMemoryId("");
        request.setSystemMessage("You are a helpful assistant");
        request.setUserMessage("Hello");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testChatEndpoint_validationError_emptySystemMessage() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setMemoryId("test-memory-1");
        request.setSystemMessage("");
        request.setUserMessage("Hello");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testChatEndpoint_validationError_emptyUserMessage() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setMemoryId("test-memory-1");
        request.setSystemMessage("You are a helpful assistant");
        request.setUserMessage("");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testChatEndpoint_validationError_nullMemoryId() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setSystemMessage("You are a helpful assistant");
        request.setUserMessage("Hello");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testChatEndpoint_validationError_nullSystemMessage() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setMemoryId("test-memory-1");
        request.setUserMessage("Hello");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testChatEndpoint_validationError_nullUserMessage() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setMemoryId("test-memory-1");
        request.setSystemMessage("You are a helpful assistant");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testChatEndpoint_validRequest_returnsStream() {
        OllamaChatRequestDTO request = new OllamaChatRequestDTO();
        request.setMemoryId("test-memory-1");
        request.setSystemMessage("You are a helpful assistant");
        request.setUserMessage("Say 'Hello'");

        webTestClient.post()
                .uri("/api/v1/ollama/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }
}
