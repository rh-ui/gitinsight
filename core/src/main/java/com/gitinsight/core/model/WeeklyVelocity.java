package com.gitinsight.core.model;

import java.time.LocalDate;

public record WeeklyVelocity(
    LocalDate weekStart,
    int commits,
    int linesAdded,
    int linesDeleted,
    int activeAuthors
) {}
