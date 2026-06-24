package com.gitinsight.core.model;

import java.time.Instant;
import java.util.List;

/**
 * A Java record is a special type of class designed to act as an immutable data carrier while eliminating repetitive boilerplate code.
 */
public record CommitInfo(
    String hash,
    String authorName,
    String authorEmail,
    Instant date,
    String message,
    List<String> changedFiles
) {}
