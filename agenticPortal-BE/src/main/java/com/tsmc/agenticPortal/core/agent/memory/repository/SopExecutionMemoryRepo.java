package com.tsmc.agenticPortal.core.agent.memory.repository;

import com.tsmc.agenticPortal.core.agent.memory.model.SopExecutionMemoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SopExecutionMemoryRepo extends JpaRepository<SopExecutionMemoryModel, String> {
}
