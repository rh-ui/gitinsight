package com.gitinsight.core.helpers;

import java.time.Instant;
import java.util.List;

import com.gitinsight.core.model.ChangeType;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;

public final class Fixtures {

    private Fixtures() {
    }

    public static CommitInfo fakeCommit(String date, String email, int added, int deleted) {
        return fakeCommit(date, "Author", email, fileChange("file.java", added, deleted));
    }

    public static CommitInfo fakeCommit(String date, String authorName, String email, FileChange... fileChanges) {
        return fakeCommit(date, authorName, email, List.of(fileChanges));
    }

    public static CommitInfo fakeCommit(String date, String authorName, String email, List<FileChange> fileChanges) {
        return new CommitInfo(
                "abc123",
                authorName,
                email,
                Instant.parse(date + "T10:00:00Z"),
                "message",
                List.copyOf(fileChanges));
    }

    public static FileChange fileChange(String path, int added, int deleted) {
        return new FileChange(path, ChangeType.MODIFY, added, deleted);
    }
}
