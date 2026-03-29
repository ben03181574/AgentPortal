package com.tsmc.agenticPortal.tools;

import com.tsmc.agenticPortal.chroma.service.ChromaEmbeddingService;
import com.tsmc.agenticPortal.sop.graph.SopState;
import com.tsmc.agenticPortal.sop.graph.SopWorkflow;
import com.tsmc.agenticPortal.sop.dao.SopGraphDAO;
import com.tsmc.agenticPortal.sop.dto.SopStepDTO;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SopTools {

    private final ChromaEmbeddingService chromaEmbeddingService;
    private final SopGraphDAO dao;
    private final SopWorkflow sopWorkflow;

    public SopTools(
            ChromaEmbeddingService chromaEmbeddingService,
            SopGraphDAO dao,
            SopWorkflow sopWorkflow) {
        this.chromaEmbeddingService = chromaEmbeddingService;
        this.dao = dao;
        this.sopWorkflow = sopWorkflow;
    }

    @Tool("使用者查詢相關 SOP 時候呼叫，輸入查詢語句，輸出最相似的 sopCode")
    public String embeddingSearchSOP(String userQuery) {
        log.info("=== [SopTools.embeddingSearchSOP]，userQuery={} ===", userQuery);

        return chromaEmbeddingService.search(userQuery);
    }

    @Tool("執行 sop 時候呼叫此函數")
    public String executeSop(@ToolMemoryId String conversationId, String sopCode, String userMessage) throws GraphStateException {

        log.info("=== [SopTools.executeSop] execute start, sopCode={} ===", sopCode);

        SopStepDTO startStep = dao.getStartStep(sopCode);
        log.info("startStep={}", startStep);

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

            log.info("======== SOP NODE ========");
            log.info("node={}", r.node());
            log.info("stepResult={}", r.state().stepResult());
            log.info("==========================");

            sopResult = r.state().sopResult();
        }

        log.info("=== [SopTools.executeSop] Sop Result: {} ===", sopResult);

        return sopResult;
    }
}