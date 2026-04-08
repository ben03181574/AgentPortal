package com.pckuow.agenticPortal.core.logging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TracingFilter implements WebFilter {

    private final Tracer tracer;

    public TracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            Span span = tracer.currentSpan();
            if (span != null && span.context() != null) {
                exchange.getResponse().getHeaders().set("X-Trace-Id", span.context().traceId());
                exchange.getResponse().getHeaders().set("X-Span-Id", span.context().spanId());
            }
            return Mono.empty();
        });

        return chain.filter(exchange);
    }
}