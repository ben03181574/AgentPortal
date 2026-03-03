package com.tsmc.agenticPortal.agent.controller;

import com.tsmc.agenticPortal.agent.service.OllamaEmbeddingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ollama")
public class OllamaEmbeddingController {

    private final OllamaEmbeddingService ollamaEmbeddingService;

    public OllamaEmbeddingController(OllamaEmbeddingService embeddingService) {
        this.ollamaEmbeddingService = embeddingService;
    }

    @GetMapping("/embedding")
    public List<Float> embed(@RequestParam("text") String text) {
        return ollamaEmbeddingService.embed(text).vectorAsList();
    }
}