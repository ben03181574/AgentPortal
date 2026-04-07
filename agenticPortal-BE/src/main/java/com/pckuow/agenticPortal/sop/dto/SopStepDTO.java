package com.pckuow.agenticPortal.sop.dto;

import lombok.Data;

import java.util.List;

@Data
public class SopStepDTO {
    public String sopCode;
    public String stepKey;
    public String name;
    public String description;
    public String stepType;
}
