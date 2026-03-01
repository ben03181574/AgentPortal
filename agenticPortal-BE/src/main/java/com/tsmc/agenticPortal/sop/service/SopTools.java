package com.tsmc.agenticPortal.sop.service;

import com.tsmc.agenticPortal.sop.dao.SopGraphDAO;
import com.tsmc.agenticPortal.sop.dto.SopStepDTO;
import com.tsmc.agenticPortal.sop.dto.SopStepInfoDTO;
import com.tsmc.agenticPortal.sop.dto.SopTemplateSummary;
import com.tsmc.agenticPortal.sop.runtime.SopExecutionState;
import com.tsmc.agenticPortal.sop.runtime.SopStateStore;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SopTools {

    private final SopGraphDAO dao;
    private final SopStateStore stateStore;

    public SopTools(SopGraphDAO dao, SopStateStore stateStore) {
        this.dao = dao;
        this.stateStore = stateStore;
    }

    @Tool("搜尋最相關的 SOP 模板。提取使用者 query 中的 keyword。回傳最多 5 筆。")
    public List<SopTemplateSummary> searchSopTemplates(String keyword) {
        List<SopTemplateSummary> ans = dao.searchTemplates(keyword, 5);
        log.info("=== [searchSopTemplates]  keyword={} , result={} ===", keyword, ans.toString());

        return ans;
    }

    @Tool("在此對話中開始執行某個 SOP（會把目前步驟設定為起始步驟）。")
    public SopStepInfoDTO startSop(@ToolMemoryId String conversationId, String sopCode) {
        SopStepDTO start = dao.getStartStep(sopCode);
        SopExecutionState st = stateStore.getOrCreate(conversationId);
        st.sopCode = sopCode;
        st.currentStepKey = start.stepKey;
        st.completed = false;
        log.info("=== [startSop] conversationId={}, sopCode={}, startStep={} ===", conversationId, sopCode, start.stepKey);

        SopStepInfoDTO stepInfo = new SopStepInfoDTO();
        stepInfo.sopStepDTO = start;
        stepInfo.sopExecutionState = st;
        return stepInfo;
    }

    @Tool("取得目前步驟內容（含下一步分支 nextOptions）。")
    public SopStepInfoDTO getCurrentStep(@ToolMemoryId String conversationId) {
        SopExecutionState st = stateStore.getOrCreate(conversationId);
        if (st.sopCode == null || st.currentStepKey == null) {
            throw new IllegalStateException("No SOP started in this conversation.");
        }
        SopStepDTO step = dao.getStep(st.sopCode, st.currentStepKey);
        log.info("=== [getCurrentStep] conversationId={}, sopCode={}, currentStepKey={} ===", conversationId, st.sopCode, st.currentStepKey);

        SopStepInfoDTO stepInfo = new SopStepInfoDTO();
        stepInfo.sopStepDTO = step;
        stepInfo.sopExecutionState = st;
        return stepInfo;
    }

    @Tool("前進到下一步：更新目前步驟為指定 stepKey，並回傳該步驟內容（含分支）。")
    public SopStepInfoDTO gotoStep(@ToolMemoryId String conversationId, String stepKey) {
        SopExecutionState st = stateStore.getOrCreate(conversationId);
        if (st.sopCode == null) {
            throw new IllegalStateException("No SOP started in this conversation.");
        }
        SopStepDTO step = dao.getStep(st.sopCode, stepKey);
        if (step.stepType.equals("END")){
            st.completed = true;
        }
        st.currentStepKey = stepKey;
        log.info("=== [gotoStep] conversationId={}, sopCode={}, stepKey={} ===", conversationId, st.sopCode, stepKey);

        SopStepInfoDTO stepInfo = new SopStepInfoDTO();
        stepInfo.sopStepDTO = step;
        stepInfo.sopExecutionState = st;
        return stepInfo;
    }

    @Tool("結束此 SOP（標記完成）。")
    public String completeSop(@ToolMemoryId String conversationId) {
        SopExecutionState st = stateStore.getOrCreate(conversationId);
        st.completed = true;
        log.info("=== [completeSop] conversationId={}, sopCode={} ===", conversationId, st.sopCode);
        return "COMPLETED " + st.sopCode + "SOP";
    }

    @Tool("把使用者提供的資訊存到 SOP 執行狀態（例如 orderId、reason、amount）。")
    public String putVar(@ToolMemoryId String conversationId, String key, String value) {
        SopExecutionState st = stateStore.getOrCreate(conversationId);
        st.vars.put(key, value);
        log.info("=== [putVar] conversationId={}, {}={} ===", conversationId, key, value);
        return "OK";
    }
}