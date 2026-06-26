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
        // SECURITY: 'path' est un chemin local arbitraire fourni par le client, ce
        // qui permet de lire n'importe quel dépôt présent sur la machine hôte.
        // Acceptable en usage local/dev ; à restreindre (allow-list / sandbox) ou à
        // remplacer par un clone d'URL distante en dossier temporaire avant prod.
        // La validation et le mapping des erreurs sont délégués (@Valid +
        // GlobalExceptionHandler) : le contrôleur ne porte aucune logique.
        return analysisService.analyze(Path.of(request.path()), request.topHotspots());
    }
}
