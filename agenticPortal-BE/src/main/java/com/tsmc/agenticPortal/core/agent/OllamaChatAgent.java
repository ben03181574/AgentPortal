package com.tsmc.agenticPortal.core.agent;

import com.tsmc.agenticPortal.core.agent.memory.OllamaChatMemoryStore;
import com.tsmc.agenticPortal.tools.SopTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OllamaChatAgent {

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
        TokenStream chat(@MemoryId String conversationId,
                          @V("systemMessage") String systemMessage,
                          @UserMessage String userMessage);
    }

    private final MeterRegistry meterRegistry;
    private final Counter chatRequestCounter;
    private final Timer chatTimer;


    public OllamaChatAgent(
            OllamaChatMemoryStore ollamaChatMemoryStore,
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
                .chatMemoryProvider(
                        memoryId -> MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(50)
                                .chatMemoryStore(ollamaChatMemoryStore)
                                .build())
                .tools(sopTools)
                .build();
    }

    public Flux<ServerSentEvent<String>> chat(String conversationId, String systemMessage, String userMessage) {
        chatRequestCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        return Flux.<ServerSentEvent<String>>create(sink -> {
            try {
                TokenStream tokenStream = assistant.chat(conversationId, systemMessage, userMessage);
                tokenStream
                    .onPartialThinking(partialThinking -> {
                        if(!sink.isCancelled()) {
                            sink.next(
                                ServerSentEvent.<String>builder()
                                    .event("thinking")
                                    .data(partialThinking.text())
                                    .build()
                            );
                        }
                    })
                    .beforeToolExecution(before -> {
                        if (!sink.isCancelled()) {
                            sink.next(
                                ServerSentEvent.<String>builder()
                                    .event("status")
                                    .data(before.request().name())
                                    .build()
                            );
                        }
                    })
                    .onToolExecuted(toolExecution -> {
                        if (!sink.isCancelled()) {
                            sink.next(
                                ServerSentEvent.<String>builder()
                                    .event("status")
                                    .data(toolExecution.request().name()+" successful executed!")
                                    .build()
                            );
                        }
                    })
                    .onPartialResponse(token -> {
                        if (!sink.isCancelled()) {
                            sink.next(
                                ServerSentEvent.<String>builder()
                                    .event("token")
                                    .data(token)
                                    .build());
                        }
                    })
                    .onCompleteResponse(response -> {
                        if (!sink.isCancelled()) {
                            sink.next(
                                ServerSentEvent.<String>builder()
                                    .event("done")
                                    .data("[DONE]")
                                    .build());
                            sink.complete();
                        }
                        sample.stop(chatTimer);
                    })
                    .onError(error -> {
                        log.error("Streaming chat failed", error);
                        if (!sink.isCancelled()) {
                            sink.next(
                                ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(error.getMessage())
                                    .build());
                            sink.complete();
                        }
                        sample.stop(chatTimer);
                    })
                    .start();
            }catch (Exception e) {
                sample.stop(chatTimer);
                sink.error(e);
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}