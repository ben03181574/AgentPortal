package com.tsmc.agenticPortal.tools;

import com.tsmc.agenticPortal.chroma.service.EmbeddingService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchTools {

    private final EmbeddingService embeddingService;

    public SearchTools(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Tool("使用者想要透過向量查詢資料庫內容時呼叫")
    public String embeddingSearch(String userQuery) {
        log.info("=== [SearchTools.embeddingSearch]，userQuery={} ===", userQuery);

        return embeddingService.search(userQuery);
    }

    @Tool("使用者想要透過建立向量資料庫資料內容時呼叫")
    public void embeddingAdd(String userQuery) {
        log.info("=== [SearchTools.embeddingAdd]，userQuery={} ===", userQuery);

        embeddingService.add(userQuery);
    }
}
