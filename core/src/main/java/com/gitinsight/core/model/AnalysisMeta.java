package com.gitinsight.core.model;

import java.time.Instant;

public record AnalysisMeta(
    int totalCommits,
    Instant firstCommit,
    Instant lastCommit,
    Instant generatedAt
) {}
