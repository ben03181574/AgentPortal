package com.pckuow.agenticPortal.chroma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChromaEmbeddingRequestDTO {

    @NotBlank(message = "embedding text cannot be blank")
    private String text;

    @NotNull(message = "metadata cannot be null")
    private Metadata metadata;

    @Data
    public static class Metadata {

        @NotBlank(message = "sopCode is required")
        private String sopCode;
    }
}
