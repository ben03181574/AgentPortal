package com.pckuow.agenticPortal.tools;

import com.pckuow.agenticPortal.chroma.service.ChromaEmbeddingService;
import com.pckuow.agenticPortal.core.logging.TraceContextHolder;
import com.pckuow.agenticPortal.sop.graph.SopState;
import com.pckuow.agenticPortal.sop.graph.SopWorkflow;
import com.pckuow.agenticPortal.sop.dao.SopGraphDAO;
import com.pckuow.agenticPortal.sop.dto.SopStepDTO;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class SopTools {

    private final ChromaEmbeddingService chromaEmbeddingService;
    private final SopGraphDAO dao;
    private final SopWorkflow sopWorkflow;
    private final Tracer tracer;
    private final TraceContextHolder traceContextHolder;

    public SopTools(
            ChromaEmbeddingService chromaEmbeddingService,
            SopGraphDAO dao,
            SopWorkflow sopWorkflow,
            Tracer tracer,
            TraceContextHolder traceContextHolder) {
        this.chromaEmbeddingService = chromaEmbeddingService;
        this.dao = dao;
        this.sopWorkflow = sopWorkflow;
        this.tracer = tracer;
        this.traceContextHolder = traceContextHolder;
    }

    @Tool("使用者查詢相關 SOP 時候呼叫，輸入查詢語句，輸出最相似的 sopCode")
    public String embeddingSearchSOP(@ToolMemoryId String conversationId, String userQuery) {

        Span parentSpan = traceContextHolder.get(conversationId);
        Span childSpan = createChildSpan(parentSpan, "tool.embeddingSearchSOP");

        try (Tracer.SpanInScope ignored = tracer.withSpan(childSpan)) {
            log.info("=== [SopTools.embeddingSearchSOP] userQuery={} ===", userQuery);
            return chromaEmbeddingService.search(userQuery);
        } catch (Exception e) {
            childSpan.error(e);
            throw e;
        } finally {
            childSpan.end();
        }
    }

    @Tool("執行 sop 時候呼叫此函數")
    public String executeSop(@ToolMemoryId String conversationId, String sopCode, String userMessage) throws GraphStateException {

        Span parentSpan = traceContextHolder.get(conversationId);
        Span childSpan = createChildSpan(parentSpan, "tool.executeSop");

        try (Tracer.SpanInScope ignored = tracer.withSpan(childSpan)) {
            SopStepDTO startStep = dao.getStartStep(sopCode);

            CompiledGraph<SopState> graph = sopWorkflow.getGraph();

            var runnableConfig = RunnableConfig.builder()
                    .threadId(conversationId)
                    .build();

            var snapshot = graph.stateOf(runnableConfig);

            Iterable<NodeOutput<SopState>> result;

            if (snapshot.isPresent()) {

                log.info("=== [SopTools.executeSop] RESUME ===");

                result = graph.stream(
                        GraphInput.resume(Map.of(
                                "userMessage", userMessage
                        )),
                        runnableConfig
                );

            } else {

                log.info("=== [SopTools.executeSop] NEW ===");

                result = graph.stream(
                        Map.of(
                                "conversationId", conversationId,
                                "sopCode", startStep.sopCode,
                                "stepKey", startStep.stepKey,
                                "userMessage", userMessage
                        ),
                        runnableConfig
                );
            }

            String sopResult = "";

            for (NodeOutput<SopState> r : result) {
                sopResult = r.state().sopResult();
            }

            return sopResult;
        }catch (Exception e) {
            childSpan.error(e);
            throw e;
        } finally {
            childSpan.end();
        }
    }

    private Span createChildSpan(Span parentSpan, String spanName) {
        Span span = (Objects.requireNonNull(parentSpan != null ? tracer.nextSpan(parentSpan) : tracer.nextSpan()))
                .name(spanName);
        return span.start();
    }
}