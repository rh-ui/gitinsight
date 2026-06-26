package com.gitinsight.api.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Corps de la requête {@code POST /api/analyze}.
 *
 * <p>
 * La validation déclarative (Bean Validation) garde le contrôleur sans logique :
 * un corps invalide est rejeté en 400 avant d'atteindre la méthode.
 */
public record AnalyzeRequest(
        @NotBlank(message = "le champ 'path' est obligatoire") String path,
        @Positive(message = "le champ 'topHotspots' doit être un entier > 0") int topHotspots) {
}
