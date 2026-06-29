package com.gitinsight.api.web.model;

/**
 * Réponse de {@code POST /api/analyze/async} : l'identifiant du job à interroger
 * ensuite via {@code GET /api/analyze/status/{jobId}}.
 */
public record JobStartResponse(String jobId) {
}
