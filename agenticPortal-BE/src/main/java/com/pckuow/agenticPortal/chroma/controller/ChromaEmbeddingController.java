package com.pckuow.agenticPortal.chroma.controller;

import com.pckuow.agenticPortal.chroma.dto.ChromaEmbeddingRequestDTO;
import com.pckuow.agenticPortal.chroma.service.ChromaEmbeddingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chroma")
public class ChromaEmbeddingController {

    private final ChromaEmbeddingService chromaEmbeddingService;

    public ChromaEmbeddingController(ChromaEmbeddingService embeddingService) {
        this.chromaEmbeddingService = embeddingService;
    }

    @PostMapping(value = "/embedding")
    public void embed(@Valid @RequestBody ChromaEmbeddingRequestDTO req) {
        Map<String, Object> map = new HashMap<>();
        map.put("sopCode", req.getMetadata().getSopCode());
        chromaEmbeddingService.add(req.getText(), map);
    }
}
