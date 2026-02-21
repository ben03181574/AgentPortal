package com.tsmc.agenticPortal.agent.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class OllamaChatRequestDTO {

    @NotBlank(message = "memory ID cannot be blank")
    private String memoryId;

    @NotBlank(message = "systemMessage cannot be blank")
    private String systemMessage;

    @NotBlank(message = "userMessage cannot be blank")
    private String userMessage;

}
