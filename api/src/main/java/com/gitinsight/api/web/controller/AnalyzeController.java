package com.gitinsight.api.web.controller;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gitinsight.api.exception.JobNotFoundException;
import com.gitinsight.api.service.AnalysisJobService;
import com.gitinsight.api.web.model.AnalyzeRequest;
import com.gitinsight.api.web.model.JobStartResponse;
import com.gitinsight.api.web.model.JobStatusResponse;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.service.AnalysisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalysisService analysisService;
    private final AnalysisJobService jobService;

    public AnalyzeController(AnalysisService analysisService, AnalysisJobService jobService) {
        this.analysisService = analysisService;
        this.jobService = jobService;
    }

    /**
     * Analyse synchrone : bloque jusqu'au résultat. Conservée pour la
     * compatibilité et les outils ; le front utilise désormais la version
     * asynchrone pour les gros dépôts.
     */
    @PostMapping("/analyze")
    public RepositoryAnalysis analyze(@Valid @RequestBody AnalyzeRequest request) throws IOException {
        Path repo = Path.of(request.path());
        return request.topCoupling() == null
                ? analysisService.analyze(repo, request.topHotspots())
                : analysisService.analyze(repo, request.topHotspots(), request.topCoupling());
    }

    /**
     * Analyse asynchrone : démarre un job en arrière-plan et rend la main
     * aussitôt (202). Le client suit l'avancement via {@link #analyzeStatus}.
     */
    @PostMapping("/analyze/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobStartResponse analyzeAsync(@Valid @RequestBody AnalyzeRequest request) {
        String jobId = jobService.submit(Path.of(request.path()), request.topHotspots(), request.topCoupling());
        return new JobStartResponse(jobId);
    }

    /** Progression d'un job ; renvoie le résultat complet une fois terminé. */
    @GetMapping("/analyze/status/{jobId}")
    public JobStatusResponse analyzeStatus(@PathVariable String jobId) {
        AnalysisJobService.Job job = jobService.get(jobId);
        if (job == null) {
            throw new JobNotFoundException(jobId);
        }
        return JobStatusResponse.from(job);
    }
}
