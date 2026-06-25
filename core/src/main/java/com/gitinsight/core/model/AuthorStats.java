package com.gitinsight.core.model;

public record AuthorStats(
        String name,
        String email,
        int commits,
        int filesTouched,
        int linesAdded,
        int linesDeleted) {}
