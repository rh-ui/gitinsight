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
import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;
import com.gitinsight.core.model.FileCoupling;
import com.gitinsight.core.model.FileOwnership;
import com.gitinsight.core.model.Hotspot;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.model.WeeklyVelocity;

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
    /**
     * Nombre d'étapes rapportées par {@link ProgressListener}. Sert à calculer un
     * pourcentage côté appelant.
     */
    private static final int TOTAL_STEPS = 6;

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

    // ── Points d'entrée ────────────────────────────────────────────────────────

    /** Surcharge de compatibilité : applique le défaut de couplage. */
    public RepositoryAnalysis analyze(Path repoPath, int topHotspots) throws IOException {
        return analyze(repoPath, topHotspots, DEFAULT_TOP_COUPLING, ProgressListener.NOOP);
    }

    public RepositoryAnalysis analyze(Path repoPath, int topHotspots, ProgressListener progress) throws IOException {
        return analyze(repoPath, topHotspots, DEFAULT_TOP_COUPLING, progress);
    }

    public RepositoryAnalysis analyze(Path repoPath, int topHotspots, int topCoupling) throws IOException {
        return analyze(repoPath, topHotspots, topCoupling, ProgressListener.NOOP);
    }

    /**
     * Cœur de l'analyse. Rapporte l'avancement à {@code progress} avant chaque
     * étape coûteuse, ce qui permet à l'appelant d'afficher une progression
     * réelle plutôt qu'un simple sablier.
     */
    public RepositoryAnalysis analyze(Path repoPath, int topHotspots, int topCoupling, ProgressListener progress)
            throws IOException {
        progress.onProgress("Lecture de l'historique", 1, TOTAL_STEPS);
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

        progress.onProgress("Vélocité", 2, TOTAL_STEPS);
        List<WeeklyVelocity> velocity = velocityCalculator.compute(commits);

        progress.onProgress("Répartition par auteur", 3, TOTAL_STEPS);
        List<AuthorStats> authors = authorStatsCalculator.compute(commits);

        progress.onProgress("Fichiers à risque", 4, TOTAL_STEPS);
        List<Hotspot> hotspots = hotspotCalculator.compute(commits, topHotspots);

        progress.onProgress("Bus factor", 5, TOTAL_STEPS);
        List<FileOwnership> busFactor = blameService.computeOwnership(repoPath, blameCandidates);

        progress.onProgress("Couplage", 6, TOTAL_STEPS);
        List<FileCoupling> coupling = couplingCalculator.compute(commits, topCoupling, MAX_FILES_PER_COMMIT);

        return new RepositoryAnalysis(meta, velocity, authors, hotspots, busFactor, coupling);
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
