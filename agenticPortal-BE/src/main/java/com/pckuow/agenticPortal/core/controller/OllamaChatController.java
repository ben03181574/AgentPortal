package com.pckuow.agenticPortal.core.controller;

import com.pckuow.agenticPortal.core.agent.OllamaChatAgent;
import com.pckuow.agenticPortal.core.dto.OllamaChatRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ollama")
public class OllamaChatController {

    private final OllamaChatAgent ollamaChatAgent;

    public OllamaChatController(OllamaChatAgent ollamaChatAgent) {
        this.ollamaChatAgent = ollamaChatAgent;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody OllamaChatRequestDTO req) {
        return ollamaChatAgent.chat(req.getMemoryId(), req.getSystemMessage(), req.getUserMessage());
    }
}
