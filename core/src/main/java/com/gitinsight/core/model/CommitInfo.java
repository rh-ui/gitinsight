package com.gitinsight.core.model;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of one Git commit and the file-level changes detected for it.
 */
public record CommitInfo(
    String hash,
    String authorName,
    String authorEmail,
    Instant date,
    String message,
    List<FileChange> fileChanges
) {}
