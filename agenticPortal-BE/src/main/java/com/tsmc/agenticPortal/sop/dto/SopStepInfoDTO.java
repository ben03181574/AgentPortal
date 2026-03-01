package com.tsmc.agenticPortal.sop.dto;

import com.tsmc.agenticPortal.sop.runtime.SopExecutionState;
import lombok.Data;

@Data
public class SopStepInfoDTO {
    public SopStepDTO sopStepDTO;
    public SopExecutionState sopExecutionState;
}
