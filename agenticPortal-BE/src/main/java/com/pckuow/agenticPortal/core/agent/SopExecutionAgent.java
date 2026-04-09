package com.pckuow.agenticPortal.core.agent;

import com.pckuow.agenticPortal.core.agent.memory.SopExecutionMemoryStore;
import com.pckuow.agenticPortal.core.logging.TraceContextHolder;
import com.pckuow.agenticPortal.tools.RefundMockTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.*;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class SopExecutionAgent {

    private final AssistantSOP assistantSOP;

    private interface AssistantSOP {
        @SystemMessage("""
                        You are an AI agent responsible for executing ONE step in a Standard Operating Procedure (SOP).
                
                        --------------------------------
                        Context
                        --------------------------------
                        - SOP Code: {{sopCode}}
                        - Step Name: {{stepName}}
                        - Step Description: {{stepDescription}}
                
                        --------------------------------
                        Your Responsibility
                        --------------------------------
                        Your job is to execute ONLY the current step.
                
                        --------------------------------
                        Tool Usage Rules (VERY IMPORTANT)
                        --------------------------------
                
                        1. You may call a tool ONLY if it matches the current step.
                        2. You MUST NOT call tools for other steps.
                        3. You MUST NOT call multiple tools in one response.
                        4. You MUST NOT skip steps.
                        5. You MUST NOT assume missing information.
                
                        --------------------------------
                        Behavior
                        --------------------------------
                
                        - If the step requires input → call the corresponding tool with the user input.
                        - If the input is missing → call the tool once, and then return the result.
                        - If the step is an action → call the action tool.
                
                        --------------------------------
                        Strict Constraints
                        --------------------------------
                
                        - You are NOT allowed to decide the workflow.
                        - You are NOT allowed to move to the next step.
                        - You are NOT allowed to call tools unrelated to the current step.
                        - You are NOT allowed to generate final answers without using tools.
                
                        --------------------------------
                        Output Rules
                        --------------------------------
                
                        - Always call ONE tool when executing a step.
                        - Return ONLY the tool result.
                        - Do NOT explain your reasoning.
                        - Do NOT add extra text.
                
                        --------------------------------
                        Goal
                        --------------------------------
                
                        Your goal is to correctly execute the current step and let the system control the workflow.
                """)
        TokenStream execute(@MemoryId String conversationId,
                             @V("sopCode") String sopCode,
                             @V("stepName") String stepName,
                             @V("stepDescription") String stepDescription,
                             @UserMessage String userMessage);
    }


    private final Tracer tracer;
    private final TraceContextHolder traceContextHolder;

    public SopExecutionAgent(
            SopExecutionMemoryStore sopExecutionMemoryStore,
            StreamingChatModel streamingChatModel,
            RefundMockTools refundMockTools, Tracer tracer, TraceContextHolder traceContextHolder) {
        this.tracer = tracer;
        this.traceContextHolder = traceContextHolder;
        this.assistantSOP = AiServices.builder(AssistantSOP.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(
                        memoryId -> MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(50)
                                .chatMemoryStore(sopExecutionMemoryStore)
                                .build())
                .tools(refundMockTools)
                .build();
    }

    public String execute(String conversationId, String sopCode, String stepName, String stepDescription, String userMessage) {
        log.info("Starting to execute step: {}", stepName);

        StringBuilder result = new StringBuilder();
        CompletableFuture<String> future = new CompletableFuture<>();

        Span parentSpan = tracer.currentSpan();
        traceContextHolder.put(conversationId, parentSpan);

        assistantSOP.execute(conversationId, sopCode, stepName, stepDescription, userMessage)
                .beforeToolExecution(beforeToolExecution -> {
                    logWithSpan(parentSpan, () -> log.info("Starting call tool: {}", beforeToolExecution.request().name()));
                })
                .onToolExecuted(toolExecution -> {
                    logWithSpan(parentSpan, () -> log.info("Ending call tool: {}", toolExecution.request().name()));
                })
                .onPartialResponse(result::append)
                .onCompleteResponse(response -> {
                    logWithSpan(parentSpan, () -> {
                        log.info("Completed step: {}", stepName);
                        future.complete(result.toString());
                    });
                })
                .onError(error -> {
                    logWithSpan(parentSpan, () -> {
                        log.error("Error while executing step: {}", stepName, error);
                        future.completeExceptionally(error);
                    });
                })
                .start();

        return future.join();
    }

    private void logWithSpan(Span span, Runnable action) {
        if (span == null) {
            action.run();
            return;
        }
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            action.run();
        }
    }
}