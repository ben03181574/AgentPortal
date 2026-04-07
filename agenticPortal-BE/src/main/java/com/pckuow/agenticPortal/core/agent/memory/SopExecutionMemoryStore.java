package com.pckuow.agenticPortal.core.agent.memory;

import com.pckuow.agenticPortal.core.agent.memory.model.SopExecutionMemoryModel;
import com.pckuow.agenticPortal.core.agent.memory.repository.SopExecutionMemoryRepo;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

@Component
@RequiredArgsConstructor
public class SopExecutionMemoryStore implements ChatMemoryStore {

    private final SopExecutionMemoryRepo sopExecutionMemoryRepo;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = String.valueOf(memoryId);

        return sopExecutionMemoryRepo.findById(id)
                .map(SopExecutionMemoryModel::getMessagesJson)
                .filter(json -> !json.isBlank())
                .map(json -> (List<ChatMessage>) messagesFromJson(json))
                .orElse(Collections.emptyList());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);
        String json = messagesToJson(messages);

        SopExecutionMemoryModel entity = sopExecutionMemoryRepo.findById(id)
                .orElseGet(() -> new SopExecutionMemoryModel(id, json));

        entity.setMessagesJson(json);
        sopExecutionMemoryRepo.save(entity);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = String.valueOf(memoryId);
        sopExecutionMemoryRepo.deleteById(id);
    }
}

