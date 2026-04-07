package com.pckuow.agenticPortal.core.agent.memory;

import com.pckuow.agenticPortal.core.agent.memory.model.OllamaChatMemoryModel;
import com.pckuow.agenticPortal.core.agent.memory.repository.OllamaChatMemoryRepo;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaChatMemoryStore implements ChatMemoryStore {

    private final OllamaChatMemoryRepo ollamaChatMemoryRepo;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = String.valueOf(memoryId);

        return ollamaChatMemoryRepo.findById(id)
                .map(OllamaChatMemoryModel::getMessagesJson)
                .filter(json -> !json.isBlank())
                .map(json -> (List<ChatMessage>) messagesFromJson(json))
                .orElse(Collections.emptyList());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);
        String json = messagesToJson(messages);

        OllamaChatMemoryModel entity = ollamaChatMemoryRepo.findById(id)
                .orElseGet(() -> new OllamaChatMemoryModel(id, json));

        entity.setMessagesJson(json);
        ollamaChatMemoryRepo.save(entity);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = String.valueOf(memoryId);
        ollamaChatMemoryRepo.deleteById(id);
    }
}
