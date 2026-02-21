package com.tsmc.agenticPortal.sop.runtime;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SopStateStore {

    private final Map<String, SopExecutionState> store = new ConcurrentHashMap<>();

    public SopExecutionState getOrCreate(String conversationId) {
        return store.computeIfAbsent(conversationId, k -> new SopExecutionState());
    }

    public void clear(String conversationId) {
        store.remove(conversationId);
    }
}