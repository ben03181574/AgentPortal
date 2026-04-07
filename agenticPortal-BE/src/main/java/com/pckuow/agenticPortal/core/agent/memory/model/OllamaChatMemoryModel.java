package com.pckuow.agenticPortal.core.agent.memory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "ollama_chat_memory")
public class OllamaChatMemoryModel {

    @Id
    @Column(name = "memory_id", nullable = false, length = 255)
    private String memoryId;

    @Column(name = "messages_json", nullable = false, columnDefinition = "LONGTEXT")
    private String messagesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OllamaChatMemoryModel(String id, String json) {
        this.memoryId = id;
        this.messagesJson = json;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
