package com.gitinsight.core.model;

import java.util.List;

public record RepositoryAnalysis(
        AnalysisMeta meta,
        List<WeeklyVelocity> velocity,
        List<AuthorStats> authors,
        List<Hotspot> hotspots,
        List<FileOwnership> busFactor,
        List<FileCoupling> coupling) {
}
