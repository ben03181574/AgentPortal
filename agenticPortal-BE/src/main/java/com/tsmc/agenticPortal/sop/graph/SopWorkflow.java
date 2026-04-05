package com.tsmc.agenticPortal.sop.graph;

import com.tsmc.agenticPortal.core.service.SopExecutionService;
import com.tsmc.agenticPortal.core.service.SopRouterService;
import com.tsmc.agenticPortal.sop.dao.SopGraphDAO;
import com.tsmc.agenticPortal.sop.dto.SopStepDTO;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class SopWorkflow {

    private final SopGraphDAO sopGraphDAO;
    private final SopExecutionService sopExecutionService;
    private final SopRouterService sopRouterService;
    private final BaseCheckpointSaver checkpointSaver;

    @Getter
    private CompiledGraph<SopState> graph;

    public SopWorkflow(SopGraphDAO sopGraphDAO,
                       SopExecutionService sopExecutionService,
                       SopRouterService sopRouterService,
                       BaseCheckpointSaver checkpointSaver) {
        this.sopGraphDAO = sopGraphDAO;
        this.sopExecutionService = sopExecutionService;
        this.sopRouterService = sopRouterService;
        this.checkpointSaver = checkpointSaver;
    }

    @PostConstruct
    public void init() throws GraphStateException {
        this.graph = buildGraphInternal();
    }

    private Map<String, Object> doAction(SopState state) {
        log.info("---Do SOP Action---");

        SopStepDTO sopStepDTO = sopGraphDAO.getStep(state.sopCode(), state.stepKey());

        String stepResult = String.format(
                "Step Name: %s%nStep Result: %s%n",
                sopStepDTO.name,
                sopExecutionService.execute(
                        state.conversationId(),
                        sopStepDTO.sopCode,
                        sopStepDTO.name,
                        sopStepDTO.description,
                        state.userMessage()
                )
        );

        String sopResult = state.sopResult() + stepResult;

        log.info("---Do SOP Action Success: {}---", state.stepKey());

        return Map.of(
                "stepResult", stepResult,
                "sopResult", sopResult
        );
    }

    private Map<String, Object> routeStep(SopState state) {
        log.info("---Route SOP Step---");

        return Map.of();
    }

    private Map<String, Object> userInput(SopState state) {
        log.info("---User Input SOP Step---");

        return Map.of();
    }

    private Map<String, Object> updateStep(SopState state) {
        log.info("---Update SOP Step---");

        SopStepDTO sopStepDTO = sopGraphDAO.getNextStep(state.sopCode(), state.stepKey());

        log.info("---Update SOP Step Success: {}---", sopStepDTO.stepKey);

        return Map.of(
                "stepKey", sopStepDTO.stepKey,
                "userMessage", "請幫我執行 SOP"
        );
    }

    private String isDone(SopState state) {
        log.info("---Check if the SOP step: {} has been done, step result: {}---", state.stepKey(), state.stepResult());

        SopRouterService.RouteType type = sopRouterService.route(state.stepResult());

        if (SopRouterService.RouteType.USER_INPUT.equals(type)) {
            return "user_input";
        }else {
            return "continue";
        }
    }

    private String isComplete(SopState state) {
        log.info("---Check if the SOP has been completed---");

        SopStepDTO sopStepDTO = sopGraphDAO.getNextStep(state.sopCode(), state.stepKey());

        if ("END".equals(sopStepDTO.stepType)) {
            return "complete";
        }

        return "not_complete";
    }

    private CompiledGraph<SopState> buildGraphInternal() throws GraphStateException {

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .interruptAfter("user_input")
                .releaseThread(true)
                .build();

        StateGraph<SopState> graph = new StateGraph<>(SopState::new)
                .addNode("do_action", node_async(this::doAction))
                .addNode("route_step", node_async(this::routeStep))
                .addNode("user_input", node_async(this::userInput))
                .addNode("update_step", node_async(this::updateStep))
                .addEdge(START, "do_action")
                .addConditionalEdges(
                        "do_action",
                        edge_async(this::isComplete),
                        Map.of(
                                "complete", END,
                                "not_complete", "route_step"
                        )
                )
                .addConditionalEdges(
                        "route_step",
                        edge_async(this::isDone),
                        Map.of(
                                "user_input", "user_input",
                                "continue", "update_step"
                        )
                )
                .addEdge("update_step", "do_action")
                .addEdge("user_input", "do_action");

        return graph.compile(compileConfig);
    }
}