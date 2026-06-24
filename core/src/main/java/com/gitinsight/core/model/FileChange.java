package com.gitinsight.core.model;

public record FileChange(String path, ChangeType type, int linesAdded, int linesDeleted) {
}
