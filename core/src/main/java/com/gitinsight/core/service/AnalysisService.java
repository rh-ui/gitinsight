package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.gitinsight.core.exception.EmptyRepositoryException;
import com.gitinsight.core.metric.AuthorStatsCalculator;
import com.gitinsight.core.metric.HotspotCalculator;
import com.gitinsight.core.metric.VelocityCalculator;
import com.gitinsight.core.model.AnalysisMeta;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.RepositoryAnalysis;

public class AnalysisService {

    private final GitHistoryService historyService;
    private final VelocityCalculator velocityCalculator;
    private final AuthorStatsCalculator authorStatsCalculator;
    private final HotspotCalculator hotspotCalculator;

    public AnalysisService() {
        this.historyService = new GitHistoryService();
        this.velocityCalculator = new VelocityCalculator();
        this.authorStatsCalculator = new AuthorStatsCalculator();
        this.hotspotCalculator = new HotspotCalculator();
    }

    public RepositoryAnalysis analyze(Path repoPath, int topHotspots) throws IOException {
        List<CommitInfo> commits = historyService.getHistory(repoPath);
        if (commits.isEmpty()) {
            throw new EmptyRepositoryException("Le depot ne contient aucun commit : " + repoPath);
        }

        Instant first = commits.stream()
                .map(CommitInfo::date)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        Instant last = commits.stream()
                .map(CommitInfo::date)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        AnalysisMeta meta = new AnalysisMeta(commits.size(), first, last, Instant.now());

        return new RepositoryAnalysis(
                meta,
                velocityCalculator.compute(commits),
                authorStatsCalculator.compute(commits),
                hotspotCalculator.compute(commits, topHotspots));
    }
}
