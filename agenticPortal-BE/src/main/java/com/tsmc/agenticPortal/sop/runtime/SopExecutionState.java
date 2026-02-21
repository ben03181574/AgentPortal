package com.tsmc.agenticPortal.sop.runtime;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SopExecutionState {
    public String sopCode;
    public String currentStepKey;
    public boolean completed;
    public Map<String, Object> vars = new HashMap<>();
}
