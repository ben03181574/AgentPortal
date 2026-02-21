package com.tsmc.agenticPortal.sop.dto;

import lombok.Data;

import java.util.List;

@Data
public class SopTemplateSummary {
    public String code;
    public String name;
    public String description;
    public List<String> tags;
}