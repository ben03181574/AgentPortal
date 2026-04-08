package com.pckuow.agenticPortal.core.logging;

import io.micrometer.tracing.Span;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TraceContextHolder {

    private final ConcurrentMap<String, Span> parentSpans = new ConcurrentHashMap<>();

    public void put(String key, Span span) {
        if (key != null && !key.isBlank() && span != null) {
            parentSpans.put(key, span);
        }
    }

    public Span get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return parentSpans.get(key);
    }

    public void remove(String key) {
        if (key != null && !key.isBlank()) {
            parentSpans.remove(key);
        }
    }
}