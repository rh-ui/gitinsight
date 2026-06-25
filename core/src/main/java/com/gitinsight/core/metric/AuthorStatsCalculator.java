package com.gitinsight.core.metric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;

public class AuthorStatsCalculator {

    public List<AuthorStats> compute(List<CommitInfo> commits) {
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, Integer> commitCount = new HashMap<>();
        Map<String, Set<String>> files = new HashMap<>();
        Map<String, Integer> added = new HashMap<>();
        Map<String, Integer> deleted = new HashMap<>();

        for (CommitInfo c : commits) {
            String email = c.authorEmail();
            names.putIfAbsent(email, c.authorName());
            commitCount.merge(email, 1, Integer::sum);
            files.computeIfAbsent(email, k -> new HashSet<>())
                 .addAll(c.fileChanges().stream().map(FileChange::path).toList());
            int la = c.fileChanges().stream().mapToInt(FileChange::linesAdded).sum();
            int ld = c.fileChanges().stream().mapToInt(FileChange::linesDeleted).sum();
            added.merge(email, la, Integer::sum);
            deleted.merge(email, ld, Integer::sum);
        }

        return names.keySet().stream()
            .map(email -> new AuthorStats(
                names.get(email),
                email,
                commitCount.get(email),
                files.get(email).size(),
                added.get(email),
                deleted.get(email)
            ))
            .sorted(Comparator.comparingInt(AuthorStats::commits).reversed())
            .toList();
    }

}
