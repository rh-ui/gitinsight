package com.gitinsight.core.model;

public record Hotspot(
    String path,
    int changeCount,
    int distinctAuthors,
    double riskScore
) {}
