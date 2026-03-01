package com.tsmc.agenticPortal.sop.controller;

import com.tsmc.agenticPortal.sop.dao.SopGraphDAO;
import com.tsmc.agenticPortal.sop.dto.SopStepDTO;
import com.tsmc.agenticPortal.sop.dto.SopTemplateSummary;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sop")
public class SopController {

    private final SopGraphDAO dao;

    public SopController(SopGraphDAO dao) {
        this.dao = dao;
    }

    @GetMapping("/searchTemplates")
    public List<SopTemplateSummary> searchTemplates(@RequestParam(required = false) String keyword) {
        return dao.searchTemplates(keyword, 5);
    }

    @GetMapping("/getStartStep")
    public SopStepDTO getStartStep(@RequestParam(required = false) String sopCode) {
        return dao.getStartStep(sopCode);
    }
}