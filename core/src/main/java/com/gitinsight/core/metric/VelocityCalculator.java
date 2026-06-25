package com.gitinsight.core.metric;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;
import com.gitinsight.core.model.WeeklyVelocity;

public class VelocityCalculator {
    public List<WeeklyVelocity> compute(List<CommitInfo> commits) {
        NavigableMap<LocalDate, List<CommitInfo>> byWeek = new TreeMap<>();
        for (CommitInfo c : commits) {
            LocalDate weekStart = toWeekStart(c.date());
            byWeek.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(c);
        }

        List<WeeklyVelocity> result = new ArrayList<>();
        if (byWeek.isEmpty()) {
            return result;
        }

        LocalDate firstWeek = byWeek.firstKey();
        LocalDate lastWeek = byWeek.lastKey();

        for (LocalDate week = firstWeek; !week.isAfter(lastWeek); week = week.plusWeeks(1)) {
            List<CommitInfo> weekCommits = byWeek.getOrDefault(week, List.of());

            int linesAdded = 0;
            int linesDeleted = 0;
            Set<String> authors = new HashSet<>();

            for (CommitInfo c : weekCommits) {
                authors.add(c.authorEmail());
                for (FileChange fc : c.fileChanges()) {
                    linesAdded += fc.linesAdded();
                    linesDeleted += fc.linesDeleted();
                }
            }

            result.add(new WeeklyVelocity(week, weekCommits.size(), linesAdded, linesDeleted, authors.size()));
        }

        return result;
    }

    private LocalDate toWeekStart(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate().with(WeekFields.ISO.dayOfWeek(), 1);
    }
}
