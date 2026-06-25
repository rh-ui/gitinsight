package com.gitinsight.core.metric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;

public class AuthorStatsCalculator {

    public List<AuthorStats> compute(List<CommitInfo> commits) {
        Map<String, Acc> authors = new HashMap<>();

        for (CommitInfo c : commits) {
            String email = normalizeEmail(c.authorEmail());
            authors.computeIfAbsent(email, key -> new Acc(c.authorName()))
                    .add(c);
        }

        return authors.entrySet().stream()
                .map(entry -> entry.getValue().toStats(entry.getKey()))
                .sorted(Comparator.comparingInt(AuthorStats::commits).reversed()
                        .thenComparing(AuthorStats::email))
                .toList();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Acc {
        private final String name;
        private final Set<String> files = new HashSet<>();
        private int commits;
        private int linesAdded;
        private int linesDeleted;

        private Acc(String name) {
            this.name = name;
        }

        private void add(CommitInfo commit) {
            commits++;
            for (FileChange fileChange : commit.fileChanges()) {
                files.add(fileChange.path());
                linesAdded += fileChange.linesAdded();
                linesDeleted += fileChange.linesDeleted();
            }
        }

        private AuthorStats toStats(String email) {
            return new AuthorStats(name, email, commits, files.size(), linesAdded, linesDeleted);
        }
    }
}
