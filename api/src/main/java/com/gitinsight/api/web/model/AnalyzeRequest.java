package com.gitinsight.api.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Corps de la requête {@code POST /api/analyze}.
 *
 * La validation déclarative (Bean Validation) garde le contrôleur sans logique :
 * un corps invalide est rejeté en 400 avant d'atteindre la méthode.
 *
 * {@code path} accepte un <b>chemin local</b> OU une <b>URL distante HTTP(S)</b>
 * (ex. {@code https://github.com/org/repo.git}) ; une URL est clonée côté serveur.
 *
 * {@code topCoupling} est <b>optionnel</b> ({@link Integer} nullable) : un client
 * existant qui ne l'envoie pas reste valide ; le défaut est appliqué côté service.
 */
public record AnalyzeRequest(
        @NotBlank(message = "le champ 'path' est obligatoire") String path,
        @Positive(message = "le champ 'topHotspots' doit être un entier > 0") int topHotspots,
        @Positive(message = "le champ 'topCoupling' doit être un entier > 0") Integer topCoupling) {
}