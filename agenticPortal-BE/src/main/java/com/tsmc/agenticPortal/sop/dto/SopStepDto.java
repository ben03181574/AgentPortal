package com.tsmc.agenticPortal.sop.dto;

import lombok.Data;

import java.util.List;

@Data
public class SopStepDto {
    public String sopCode;
    public String stepKey;
    public String name;
    public String description;
    public String stepType;
    public List<NextOption> nextOptions;

    public static class NextOption {
        public String nextStepKey;
        public String conditionType;   // ALWAYS / IF / ELSE
        public String conditionText;
    }
}
