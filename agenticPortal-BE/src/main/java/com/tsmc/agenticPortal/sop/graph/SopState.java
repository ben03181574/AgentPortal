package com.tsmc.agenticPortal.sop.graph;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;

public class SopState extends AgentState {
    public SopState(Map<String, Object> initData) {
        super(initData);
    }

    public String conversationId(){
        Optional<String> conversationId = value("conversationId");
        return conversationId.orElseThrow( () -> new IllegalArgumentException("conversationId not found"));
    }

    public String sopCode() {
        Optional<String> sopCode = value("sopCode");
        return sopCode.orElseThrow( () -> new IllegalArgumentException("SopCode not found!"));
    }

    public String stepKey() {
        Optional<String> stepKey = value("stepKey");
        return stepKey.orElseThrow( () -> new IllegalArgumentException("StepKey not found!"));
    }

    public String userMessage() {
        Optional<String> userMessage = value("userMessage");
        return userMessage.orElse("請幫我執行 SOP");
    }

    public String stepResult() {
        Optional<String> stepResult = value("stepResult");
        return stepResult.orElse("");
    }

    public String sopResult() {
        Optional<String> sopResult = value("sopResult");
        return sopResult.orElse("");
    }
}
