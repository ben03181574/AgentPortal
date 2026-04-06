package com.tsmc.agenticPortal.core.agent.memory.repository;

import com.tsmc.agenticPortal.core.agent.memory.model.OllamaChatMemoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OllamaChatMemoryRepo extends JpaRepository<OllamaChatMemoryModel, String> {
}