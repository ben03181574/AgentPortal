package com.tsmc.agenticPortal.core.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SopRouterService {

    public enum RouteType {
        USER_INPUT,
        CONTINUE
    }

    public static class RouteResult {
        @Description("Decide whether to wait for user input or continue the SOP.")
        public RouteType action;
    }

    private final AssistantRouter assistantRouter;
    private interface AssistantRouter {

        @SystemMessage("""
        You are an expert SOP step status classifier.
        
        Your job is to determine whether the CURRENT step has been completed or still requires user input.
        
        You will receive ONLY the latest step result.
        
        --------------------------------
        Definitions:
        --------------------------------
        
        USER_INPUT:
        - The step is NOT completed
        - The system is still asking the user for required information
        - Example:
          "Please provide your order number"
          "Please describe your issue"
        
        CONTINUE:
        - The step is ALREADY completed
        - The system has received the user's input
        - The step can move forward
        - Example:
          "Received your order number"
          "Information has been collected successfully"
        
        --------------------------------
        Important Rules:
        --------------------------------
        
        - Focus ONLY on the FINAL outcome of the step
        - Ignore any previous instructions or prompts
        - If the message indicates the system has ALREADY received input → CONTINUE
        - If the message is STILL asking for input → USER_INPUT
        
        --------------------------------
        Output Rules:
        --------------------------------
        
        - Return ONLY structured result
        - Do NOT explain
        """)
        Result<RouteResult> route(@UserMessage String stepResult);
    }

    public SopRouterService(ChatModel chatModel) {
        this.assistantRouter = AiServices.builder(AssistantRouter.class)
                .chatModel(chatModel)
                .build();
    }

    public RouteType route(String stepResult) {
        Result<RouteResult> result = assistantRouter.route(stepResult);
        log.info("=== [SopRouter] decision: {}, reason: {} ===", result.content().action, result.finishReason());
        return result.content().action;
    }
}
