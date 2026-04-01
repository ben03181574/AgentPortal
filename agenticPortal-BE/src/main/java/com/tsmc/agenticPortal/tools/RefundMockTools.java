package com.tsmc.agenticPortal.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefundMockTools {

    @Tool("""
        Find order information by order ID in a mock way.
        Use this tool for the ASK_ORDER_ID step in SIMPLE_REFUND_FLOW.
        After the user provides an order ID, call this tool to verify whether the order exists
        and return mock order information.
        """)
    public String mockFindOrderByOrderId(String orderId) {
        log.info("=== [RefundMockTools.mockFindOrderByOrderId] start, orderId={} ===", orderId);

        if (orderId == null || orderId.isBlank()) {
            log.info("=== [RefundMockTools.mockFindOrderByOrderId] invalid orderId ===");
            return "Order lookup failed: orderId must not be empty.";
        }

        String result = """
                Order lookup success:
                Order ID: %s
                Order Status: PAID
                Product Name: Mock Product A
                Order Amount: 1000
                Refundable: YES
                """.formatted(orderId);

        log.info("=== [RefundMockTools.mockFindOrderByOrderId] success, result={} ===", result);
        return result;
    }

    @Tool("""
        Validate the refund reason in a mock way.
        Use this tool for the ASK_REASON step in SIMPLE_REFUND_FLOW.
        After the user provides a refund reason, call this tool to perform a mock validation
        and return whether the reason is acceptable.
        """)
    public String mockValidateRefundReason(String reason) {
        log.info("=== [RefundMockTools.mockValidateRefundReason] start, reason={} ===", reason);

        if (reason == null || reason.isBlank()) {
            log.info("=== [RefundMockTools.mockValidateRefundReason] invalid reason ===");
            return "Refund reason validation failed: reason must not be empty.";
        }

        String normalized = reason.trim();

        String result = """
                Refund reason validation success:
                Refund Reason: %s
                Validation Result: APPROVED
                Note: This refund reason matches the mock validation rules.
                """.formatted(normalized);

        log.info("=== [RefundMockTools.mockValidateRefundReason] success, result={} ===", result);
        return result;
    }

    @Tool("""
        Execute a refund in a mock way.
        Use this tool for the DO_REFUND step in SIMPLE_REFUND_FLOW.
        This tool requires an order ID and a refund reason,
        and returns a mock refund execution result without calling any real external system.
        """)
    public String mockExecuteRefund(String orderId, String reason) {
        log.info("=== [RefundMockTools.mockExecuteRefund] start, orderId={}, reason={} ===", orderId, reason);

        if (orderId == null || orderId.isBlank()) {
            log.info("=== [RefundMockTools.mockExecuteRefund] failed, empty orderId ===");
            return "Refund execution failed: orderId must not be empty.";
        }

        if (reason == null || reason.isBlank()) {
            log.info("=== [RefundMockTools.mockExecuteRefund] failed, empty reason ===");
            return "Refund execution failed: reason must not be empty.";
        }

        String refundNo = "RF-" + System.currentTimeMillis();

        String result = """
                Refund execution success:
                Order ID: %s
                Refund Reason: %s
                Refund Transaction ID: %s
                Refund Status: SUCCESS
                """.formatted(orderId, reason, refundNo);

        log.info("=== [RefundMockTools.mockExecuteRefund] success, result={} ===", result);
        return result;
    }

    @Tool("""
        Query refund result in a mock way.
        Use this tool after the refund has been executed
        if you want to confirm the final refund status.
        """)
    public String mockQueryRefundResult(String refundTransactionId) {
        log.info("=== [RefundMockTools.mockQueryRefundResult] start, refundTransactionId={} ===", refundTransactionId);

        if (refundTransactionId == null || refundTransactionId.isBlank()) {
            log.info("=== [RefundMockTools.mockQueryRefundResult] invalid refundTransactionId ===");
            return "Refund query failed: refundTransactionId must not be empty.";
        }

        String result = """
                Refund query success:
                Refund Transaction ID: %s
                Refund Status: SUCCESS
                Settlement Status: COMPLETED
                """.formatted(refundTransactionId);

        log.info("=== [RefundMockTools.mockQueryRefundResult] success, result={} ===", result);
        return result;
    }
}