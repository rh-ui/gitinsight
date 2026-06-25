package com.gitinsight.core.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gitinsight.core.helpers.Fixtures;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.Hotspot;

class HotspotCalculatorTest {
    private final HotspotCalculator calculator = new HotspotCalculator();

    @Test
    void fileModifiedOftenRanksHigh() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-09", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-10", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-11", "Alice", "alice@example.com",
                        Fixtures.fileChange("Readme.md", 1, 0)));

        List<Hotspot> result = calculator.compute(commits, 10);

        assertThat(result).extracting(Hotspot::path)
                .containsExactly("Main.java", "Readme.md");
        assertThat(result).extracting(Hotspot::changeCount)
                .containsExactly(3, 1);
    }

    @Test
    void manyAuthorsIncreasesRiskScore() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 1, 0),
                        Fixtures.fileChange("Config.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-09", "Bob", "bob@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-10", "Alice", "alice@example.com",
                        Fixtures.fileChange("Config.java", 1, 0)));

        List<Hotspot> result = calculator.compute(commits, 10);

        assertThat(result).extracting(Hotspot::path)
                .containsExactly("Main.java", "Config.java");
        assertThat(result).extracting(Hotspot::changeCount)
                .containsExactly(2, 2);
        assertThat(result).extracting(Hotspot::distinctAuthors)
                .containsExactly(2, 1);
    }

    @Test
    void topNLimitsResults() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("First.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-09", "Alice", "alice@example.com",
                        Fixtures.fileChange("First.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-10", "Bob", "bob@example.com",
                        Fixtures.fileChange("First.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-11", "Bob", "bob@example.com",
                        Fixtures.fileChange("Second.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-12", "Bob", "bob@example.com",
                        Fixtures.fileChange("Second.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-13", "Carol", "carol@example.com",
                        Fixtures.fileChange("Third.java", 1, 0)));

        List<Hotspot> result = calculator.compute(commits, 2);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Hotspot::path)
                .containsExactly("First.java", "Second.java");
    }

    @Test
    void riskScoreIsChangeCountTimesAuthors() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-09", "Bob", "bob@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-10", "Carol", "carol@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-11", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 1, 0)));

        Hotspot hotspot = calculator.compute(commits, 10).get(0);

        assertThat(hotspot.path()).isEqualTo("Main.java");
        assertThat(hotspot.changeCount()).isEqualTo(4);
        assertThat(hotspot.distinctAuthors()).isEqualTo(3);
        assertThat(hotspot.riskScore()).isEqualTo(12.0);
    }
}
