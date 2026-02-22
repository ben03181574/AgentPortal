package com.tsmc.agenticPortal.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class RefundDomainTools {


    @Tool("檢查指定訂單是否符合退款條件，輸入參數是訂單編號以及價格，輸出是否符合條件。")
    public Boolean checkRefundEligibility(String orderId, BigDecimal amount) {
        log.info("=== [RefundDomainTools.checkRefundEligibility]，orderId={}, amount={} ===", orderId, amount);

        return amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("1000")) < 0;
    }

    @Tool("對指定訂單執行退款。")
    public String performRefund(String orderId, BigDecimal amount, String reason) {
        log.info("=== [RefundDomainTools.performRefund]，orderId={}, amount={}, reason={} ===",
                orderId, amount, reason);

        return "REFUND_OK" + reason;
    }

    @Tool("記錄退款被拒絕，並保存拒絕原因。")
    public String recordRefundRejection(String orderId, String reason) {
        log.info("=== [RefundDomainTools.recordRefundRejection]，orderId={}, reason={} ===",
                orderId, reason);

        return "REJECT_RECORDED" + reason;
    }

    @Tool("通知使用者退款結果。")
    public String notifyUserRefundResult(String orderId, String result, String messageToUser) {
        log.info("=== [RefundDomainTools.notifyUserRefundResult]，orderId={}, result={}, messageToUser={} ===",
                orderId, result, messageToUser);

        return "NOTIFY_SENT" + messageToUser;
    }
}
