package com.pckuow.agenticPortal.core.agent;

import com.pckuow.agenticPortal.core.agent.memory.SopExecutionMemoryStore;
import com.pckuow.agenticPortal.tools.RefundMockTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
        Flux<String> execute(@MemoryId String conversationId,
                             @V("sopCode") String sopCode,
                             @V("stepName") String stepName,
                             @V("stepDescription") String stepDescription,
                             @UserMessage String userMessage);
    }

    public SopExecutionAgent(
            SopExecutionMemoryStore sopExecutionMemoryStore,
            StreamingChatModel streamingChatModel,
            RefundMockTools refundMockTools) {
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
        return Flux.defer(() -> assistantSOP.execute(conversationId, sopCode, stepName, stepDescription, userMessage))
                .collectList()
                .map(list -> String.join("", list))
                .retry(3)
                .block();
    }
}