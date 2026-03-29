package com.tsmc.agenticPortal.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Component
@Slf4j
public class MachineTools {

    @Tool("Check machine ID, the input is ID of machine.")
    public String checkMachineId(String machineId) {
        log.info("Check machine ID: {}", machineId);

        if (machineId == null || machineId.isBlank()) {
            return "[ASK_MACHINE_ID] Please provide the machine ID.";
        }

        return "[DONE] Machine ID: " + machineId;
    }

    @Tool("Check operator ID, the input is ID of operator")
    public String checkOperator(String operatorId) {
        log.info("Check operator ID: {}", operatorId);

        if (operatorId == null || operatorId.isBlank()) {
            return "[ASK_OPERATOR] Please provide the operator ID.";
        }

        return "[DONE] Operator ID: " + operatorId;
    }

    @Tool("Start machine")
    public String startMachine() {

        return "[DONE] Machine started successfully.";
    }
}