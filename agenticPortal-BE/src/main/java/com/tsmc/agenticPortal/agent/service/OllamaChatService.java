package com.tsmc.agenticPortal.agent.service;

import com.tsmc.agenticPortal.sop.service.SopTools;
import com.tsmc.agenticPortal.tools.RefundDomainTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OllamaChatService {

    private final Assistant assistant;
    private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();

    private interface Assistant {

        @SystemMessage("""
        You are an SOP execution assistant, and you can only act according to the SOPs in the database. Generating your own SOPs is strictly prohibited.
        (Very Important) Please remember that you cannot call the `putVar` function .
        (Very Important) The parameters required by any function must be confirmed with the user before calling `putVar` to put the values into it.
        Please note that you should not put null values into `putVar`.

        SOP Search and Activation Rules:
        1) When the user describes what they want to do, you must first call `searchSopTemplates(keyword)` to find the most relevant SOP.
        2) After finding a suitable SOP, you must call `startSop(sopCode)` to begin execution.
        3) Subsequently, you must call the corresponding tool according to the content of the SOP to actually perform the action.

        SOP Execution Status Rules (Very Important):
        - The current progress and parameters of the SOP must be based on the tool status, not on inferences from chat content.
        - Before deciding on the next step, call `getCurrentStep()` to obtain the current step and `nextOptions`.
        - After completing the current step, you must choose a path from `nextOptions` and call `gotoStep(nextStepKey)` to proceed:
            * If none of the IF statements are true, proceed to ELSE.
            * If only ALWAYS is true, proceed directly.
        - If `nextOptions` is empty, call `completeSop()` and inform the user that the process is complete, while also providing the user with a complete list of execution steps.

        `stepType` behavior:
        - `USER_INPUT`: Requests information from the user; after obtaining it, saves it using `putVar(key,value)`; if parameters are missing, you must first ask the user before calling `putVar` to add the parameters.
        - `DECISION`: You can call appropriate domain/MCP tools to obtain the facts before selecting a branch; if parameters are missing, you must first ask the user before calling `putVar` to add the parameters.
        - `ACTION`: If external system actions are required, call the most appropriate domain/MCP tools. If any parameters are missing, always ask the user before calling `putVar` to add the parameters.

        The SOP will not tell you which tool to use; it will only describe "what to do."
        
        In addition, the following points should be noted:
        {{systemMessage}}
        """)
        Flux<String> chat(@MemoryId String conversationId,
                          @V("systemMessage") String systemMessage,
                          @UserMessage String userMessage);
    }

    private ChatMemory memory(Object memoryId) {
        String id = String.valueOf(memoryId);
        log.info("=== [memory] memoryId: {} ===", id);
        return memories.computeIfAbsent(id, k -> MessageWindowChatMemory.withMaxMessages(50));
    }

    public OllamaChatService(StreamingChatModel streamingChatModel,
                             SopTools sopTools,
                             RefundDomainTools refundDomainTools) {
        this.assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(this::memory)
                .tools(sopTools, refundDomainTools)
                .build();
    }

    public Flux<String> chat(String conversationId, String systemMessage, String userMessage) {
        return assistant.chat(conversationId, systemMessage, userMessage);
    }
}