package com.tsmc.agenticPortal.agent.service;

import com.tsmc.agenticPortal.sop.service.SopTools;
import com.tsmc.agenticPortal.tools.RefundDomainTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OllamaChatService {

    private final Assistant assistant;
    private final Map<String, ChatMemory> memories = new ConcurrentHashMap<>();

    private interface Assistant {

        @SystemMessage("""
        你是一個 SOP 執行助理，只能按照資料庫中的 SOP 做事，絕對禁止自己生成 SOP，
        同時一定要把執行過的每個動作回應給使用者，完整的執行步驟都要回應給使用者，
        並且注意如果缺乏 TOOL 所需要的參數，必須要向使用者提問，絕不能自己生成。
        
        SOP 尋找與啟動規則：
        1) 使用者描述想做的事時，你必須先呼叫 searchSopTemplates(keyword) 找到最相關 SOP。
        2) 找到合適 SOP 後，你必須呼叫 startSop(sopCode) 才算開始執行。
        3) 接著後續必須要根據 SOP 的內容呼叫相對應的 TOOL 來實際執行動作
        4) 每步都要檢查是否 StepType 為 'END'，如果不是則代表此流程尚未結束，不得中斷或自行結束
        
        SOP 執行狀態規則（非常重要）：
        - SOP 的目前進度與參數必須以工具狀態為準，不可只靠聊天內容推測。
        - 每次決定下一步前，先呼叫 getCurrentStep() 取得目前步驟與 nextOptions。
        - 完成目前步驟後，必須從 nextOptions 選一條路並呼叫 gotoStep(nextStepKey) 前進：
          * 優先判斷 IF（多條 IF 用 priority 小的優先）
          * IF 都不成立則走 ELSE
          * 只有 ALWAYS 就直接走
        - 必須要走到 stepType 為 'END' 才能結束並呼叫 completeSop()，並向使用者說明流程完成，同時將完整的執行步驟都要回應給使用者。
        
        stepType 行為：
        - USER_INPUT：需要資訊就詢問使用者；取得後用 putVar(key,value) 保存。
        - DECISION：你可以呼叫合適的 domain/MCP tools 取得事實後再選分支；如果有缺乏參數一定要先問使用者，絕對不要自己生成。
        - ACTION：若需要外部系統動作，呼叫最合適的 domain/MCP tools；缺參數先問人。
        - END: 代表此 SOP 流程已經結束，可以將結果總結給使用者。
        
        SOP 不會告訴你要用哪個 tool，只會描述「要做什麼」。
        - 你的工作是根據步驟 description 與你擁有的 tools 的用途，自己決定是否呼叫，並且注意如果缺乏 TOOL 所需要的參數，必須要向使用者提問，絕不能自己生成。
        - 請根據 TOOL 所需要的參數從 SopExecutionState 中獲取，如果沒有必須要向使用者詢問並確認，絕對不能自己生成。
        
        另外也須注意以下事項：
        {{systemMessage}}
        """)
        Flux<String> chat(@MemoryId String conversationId,
                          @V("systemMessage") String systemMessage,
                          @UserMessage String userMessage);
    }

    private ChatMemory memory(Object memoryId) {
        String id = String.valueOf(memoryId);
        log.info("=== [memory] memoryId: {} ===", id);
        return memories.computeIfAbsent(id, k -> MessageWindowChatMemory.withMaxMessages(50));
    }

    public OllamaChatService(StreamingChatModel streamingChatModel,
                             SopTools sopTools,
                             RefundDomainTools refundDomainTools) {
        this.assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(this::memory)
                .tools(sopTools, refundDomainTools)
                .build();
    }

    public Flux<String> chat(String conversationId, String systemMessage, String userMessage) {
        return assistant.chat(conversationId, systemMessage, userMessage);
    }
}