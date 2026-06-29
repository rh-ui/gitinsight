package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gitinsight.core.exception.EmptyRepositoryException;
import com.gitinsight.core.metric.AuthorStatsCalculator;
import com.gitinsight.core.metric.CouplingCalculator;
import com.gitinsight.core.metric.HotspotCalculator;
import com.gitinsight.core.metric.VelocityCalculator;
import com.gitinsight.core.model.AnalysisMeta;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;
import com.gitinsight.core.model.RepositoryAnalysis;

public class AnalysisService {

    /**
     * Défaut de couplage quand l'appelant ne le précise pas (CLI, ancien front).
     */
    private static final int DEFAULT_TOP_COUPLING = 30;
    /**
     * Borne perf du bus factor : on ne blâme que les N fichiers les plus modifiés.
     */
    private static final int BLAME_CANDIDATES = 50;
    /**
     * Au-delà, un commit ne contribue pas au pairage de couplage (merges/massifs).
     */
    private static final int MAX_FILES_PER_COMMIT = 50;

    private final GitHistoryService historyService;
    private final VelocityCalculator velocityCalculator;
    private final AuthorStatsCalculator authorStatsCalculator;
    private final HotspotCalculator hotspotCalculator;
    private final BlameService blameService;
    private final CouplingCalculator couplingCalculator;

    public AnalysisService() {
        this.historyService = new GitHistoryService();
        this.velocityCalculator = new VelocityCalculator();
        this.authorStatsCalculator = new AuthorStatsCalculator();
        this.hotspotCalculator = new HotspotCalculator();
        this.blameService = new BlameService();
        this.couplingCalculator = new CouplingCalculator();
    }

    /** Surcharge de compatibilité : applique le défaut de couplage. */
    public RepositoryAnalysis analyze(Path repoPath, int topHotspots) throws IOException {
        return analyze(repoPath, topHotspots, DEFAULT_TOP_COUPLING);
    }

    public RepositoryAnalysis analyze(Path repoPath, int topHotspots, int topCoupling) throws IOException {
        List<CommitInfo> commits = historyService.getHistory(repoPath);
        if (commits.isEmpty()) {
            throw new EmptyRepositoryException("Le depot ne contient aucun commit : " + repoPath);
        }

        Instant first = commits.stream().map(CommitInfo::date).min(Comparator.naturalOrder()).orElseThrow();
        Instant last = commits.stream().map(CommitInfo::date).max(Comparator.naturalOrder()).orElseThrow();
        AnalysisMeta meta = new AnalysisMeta(commits.size(), first, last, Instant.now());

        // Candidate-set borné pour le blame : les fichiers les plus modifiés.
        // BlameService ignorera ceux qui ont disparu du HEAD.
        List<String> blameCandidates = mostChangedPaths(commits, BLAME_CANDIDATES);

        return new RepositoryAnalysis(
                meta,
                velocityCalculator.compute(commits),
                authorStatsCalculator.compute(commits),
                hotspotCalculator.compute(commits, topHotspots),
                blameService.computeOwnership(repoPath, blameCandidates),
                couplingCalculator.compute(commits, topCoupling, MAX_FILES_PER_COMMIT));
    }

    /** Les {@code limit} fichiers les plus modifiés (par nombre de changements). */
    private List<String> mostChangedPaths(List<CommitInfo> commits, int limit) {
        Map<String, Integer> changesPerFile = new HashMap<>();
        for (CommitInfo commit : commits) {
            for (FileChange change : commit.fileChanges()) {
                changesPerFile.merge(change.path(), 1, Integer::sum);
            }
        }
        return changesPerFile.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<String, Integer>comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}