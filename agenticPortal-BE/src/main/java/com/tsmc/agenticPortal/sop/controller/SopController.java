package com.tsmc.agenticPortal.sop.controller;

import com.tsmc.agenticPortal.sop.dao.SopGraphDao;
import com.tsmc.agenticPortal.sop.dto.SopStepDto;
import com.tsmc.agenticPortal.sop.dto.SopTemplateSummary;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sop")
public class SopController {

    private final SopGraphDao dao;

    public SopController(SopGraphDao dao) {
        this.dao = dao;
    }

    @GetMapping("/searchTemplates")
    public List<SopTemplateSummary> searchTemplates(@RequestParam(required = false) String keyword) {
        return dao.searchTemplates(keyword, 5);
    }

    @GetMapping("/getStartStep")
    public SopStepDto getStartStep(@RequestParam(required = false) String sopCode) {
        return dao.getStartStep(sopCode);
    }
}