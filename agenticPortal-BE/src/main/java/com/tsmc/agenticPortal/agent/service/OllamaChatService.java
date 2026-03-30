package com.tsmc.agenticPortal.agent.service;

import com.tsmc.agenticPortal.tools.SopTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OllamaChatService {

    private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();
    private ChatMemory memory(Object memoryId) {
        String id = String.valueOf(memoryId);
        log.info("=== [memory] memoryId: {} ===", id);
        return memories.computeIfAbsent(id, k -> MessageWindowChatMemory.withMaxMessages(50));
    }

    private final Assistant assistant;
    private interface Assistant {
        @SystemMessage("""
        You are the main orchestration assistant responsible for coordinating SOP execution and general conversation.
        
        Your responsibilities:
        - Understand the user's intent
        - Decide whether to start, continue, or ignore an SOP
        - Use tools to execute SOPs when needed
        - Answer general questions when SOP is not relevant
        
        --------------------------------
        SOP Awareness Rules (CRITICAL)
        --------------------------------
        
        There may be an ongoing SOP workflow in progress.
        
        When a SOP is in progress, you MUST decide whether the user's latest message is:
        
        1. CONTINUE_SOP
           - The user is providing requested information
           - The user is answering a previous question from the SOP
           - The user is clearly continuing the workflow
        
        2. INTERRUPT_SOP
           - The user asks an unrelated question
           - The user changes topic
           - The user does not intend to continue the SOP
        
        3. UNCLEAR
           - The message is ambiguous
           - It is unclear whether the user is continuing the SOP
        
        --------------------------------
        Behavior Rules
        --------------------------------
        
        If CONTINUE_SOP:
        - Call executeSop to continue the workflow
        - Do NOT manually simulate or generate SOP steps
        
        If INTERRUPT_SOP:
        - DO NOT call executeSop
        - Answer the user's question normally
        - Keep the SOP state unchanged (it may resume later)
        
        If UNCLEAR:
        - Ask a clarification question
        - DO NOT call executeSop yet
        
        --------------------------------
        Starting SOP
        --------------------------------
        
        If there is NO ongoing SOP:
        - If the user is requesting a process/workflow/task:
          1. Call embeddingSearchSOP
          2. Then call executeSop (ONLY ONCE)
        
        --------------------------------
        Tool Usage Constraints
        --------------------------------
        
        - executeSop can be called at most ONCE per user message
        - Never call executeSop repeatedly in the same turn
        - Never simulate SOP steps manually
        - Always rely on tools for SOP execution
        
        --------------------------------
        Response Formatting
        --------------------------------
        
        - Always present SOP results clearly in step-by-step format
        - Do not modify or invent step results
        - If waiting for input, clearly tell the user what is needed
        - If answering unrelated questions, respond naturally
        
        --------------------------------
        Key Principle
        --------------------------------
        
        You are NOT the workflow engine.
        You are ONLY the decision-maker of whether to use the workflow engine.
        {{systemMessage}}
        """)
        Flux<String> chat(@MemoryId String conversationId,
                          @V("systemMessage") String systemMessage,
                          @UserMessage String userMessage);
    }

    private final MeterRegistry meterRegistry;
    private final Counter chatRequestCounter;
    private final Timer chatTimer;


    public OllamaChatService(
            StreamingChatModel streamingChatModel,
            SopTools sopTools,
            MeterRegistry meterRegistry) {

        this.meterRegistry = meterRegistry;
        this.chatRequestCounter = Counter.builder("agentportal_chat_requests_total")
                .description("Total chat requests")
                .register(meterRegistry);
        this.chatTimer = Timer.builder("agentportal_chat_latency")
                .description("Chat end-to-end latency")
                .register(meterRegistry);

        this.assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(this::memory)
                .tools(sopTools)
                .build();
    }

    public Flux<String> chat(String conversationId, String systemMessage, String userMessage) {
        chatRequestCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        return assistant.chat(conversationId, systemMessage, userMessage)
                .doOnTerminate(() -> sample.stop(chatTimer));
    }
}