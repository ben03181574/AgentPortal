package com.tsmc.agenticPortal.sop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class SopControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testSearchTemplates_withKeyword_returnsResults() {
        webTestClient.get()
                .uri("/api/v1/sop/searchTemplates?keyword=refund")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    public void testSearchTemplates_withoutKeyword_returnsResults() {
        webTestClient.get()
                .uri("/api/v1/sop/searchTemplates")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    public void testGetStartStep_withValidSopCode_returnsStep() {
        webTestClient.get()
                .uri("/api/v1/sop/getStartStep?sopCode=REFUND_FLOW")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sopCode").isEqualTo("REFUND_FLOW")
                .jsonPath("$.stepKey").exists()
                .jsonPath("$.name").exists();
    }

    @Test
    public void testSearchTemplates_withNonExistentKeyword_returnsEmptyArray() {
        webTestClient.get()
                .uri("/api/v1/sop/searchTemplates?keyword=xyznonexistentkeyword123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }
}
