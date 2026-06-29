package com.gitinsight.api.web.model;

import com.gitinsight.api.service.AnalysisJobService;
import com.gitinsight.core.model.RepositoryAnalysis;

/**
 * État renvoyé par {@code GET /api/analyze/status/{jobId}}.
 *
 * <p>
 * {@code analysis} n'est renseigné que lorsque {@code status == "DONE"} ;
 * {@code message} n'est renseigné que lorsque {@code status == "ERROR"}. Les
 * autres cas laissent ces champs à {@code null}.
 */
public record JobStatusResponse(
        String status,
        String step,
        int current,
        int total,
        RepositoryAnalysis analysis,
        String message) {

    public static JobStatusResponse from(AnalysisJobService.Job job) {
        return new JobStatusResponse(
                job.status().name(),
                job.step(),
                job.current(),
                job.total(),
                job.result(),
                job.error());
    }
}
