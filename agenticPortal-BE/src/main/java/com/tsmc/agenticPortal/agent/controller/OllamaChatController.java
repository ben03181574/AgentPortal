package com.tsmc.agenticPortal.agent.controller;

import com.tsmc.agenticPortal.agent.service.OllamaChatService;
import com.tsmc.agenticPortal.agent.dto.OllamaChatRequestDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ollama")
public class OllamaChatController {

    private final OllamaChatService ollamaChatService;

    public OllamaChatController(OllamaChatService ollamaChatService) {
        this.ollamaChatService = ollamaChatService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@Valid @RequestBody OllamaChatRequestDTO req) {
        return ollamaChatService.chat(req.getMemoryId(), req.getSystemMessage(), req.getUserMessage());
    }
}
