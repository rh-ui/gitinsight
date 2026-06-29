package com.gitinsight.api.web.controller;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gitinsight.api.web.model.AnalyzeRequest;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.service.AnalysisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalysisService analysisService;

    public AnalyzeController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public RepositoryAnalysis analyze(@Valid @RequestBody AnalyzeRequest request) throws IOException {
        Path repo = Path.of(request.path());
        return request.topCoupling() == null
                ? analysisService.analyze(repo, request.topHotspots())
                : analysisService.analyze(repo, request.topHotspots(), request.topCoupling());
    }
}
