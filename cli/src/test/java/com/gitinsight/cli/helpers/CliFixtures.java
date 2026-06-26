package com.gitinsight.cli.helpers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.gitinsight.core.model.AnalysisMeta;
import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.Hotspot;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.model.WeeklyVelocity;

public class CliFixtures {

    public static RepositoryAnalysis sampleAnalysis() {
        var meta = new AnalysisMeta(
            42,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-06-01T00:00:00Z"),
            Instant.now()
        );

        var velocity = List.of(
            new WeeklyVelocity(
                LocalDate.parse("2026-05-01"),
                20, 400, 100, 3        // semaine active
            ),
            new WeeklyVelocity(
                LocalDate.parse("2026-05-08"),
                5, 80, 20, 1          // semaine calme
            )
        );

        var authors = List.of(
            new AuthorStats("Alice", "alice@example.com", 30, 12, 350, 80),
            new AuthorStats("Bob", "bob@example.com", 12, 5, 120, 40)
        );

        var hotspots = List.of(
            new Hotspot("src/main/java/com/gitinsight/core/service/AnalysisService.java",
                        18, 4, 8.5),
            new Hotspot("src/main/java/com/gitinsight/core/model/CommitData.java",
                        7, 2, 3.2)
        );

        return new RepositoryAnalysis(meta, velocity, authors, hotspots);
    }
}
