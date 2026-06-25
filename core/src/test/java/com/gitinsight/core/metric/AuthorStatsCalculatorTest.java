package com.gitinsight.core.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.gitinsight.core.helpers.Fixtures;
import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.CommitInfo;

class AuthorStatsCalculatorTest {
    private final AuthorStatsCalculator calculator = new AuthorStatsCalculator();

    @Test
    void countsCommitsPerAuthor() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 10, 2)),
                Fixtures.fakeCommit("2024-01-09", "Bob", "bob@example.com",
                        Fixtures.fileChange("Api.java", 5, 1)),
                Fixtures.fakeCommit("2024-01-10", "Alice", "alice@example.com",
                        Fixtures.fileChange("Service.java", 3, 0)));

        List<AuthorStats> result = calculator.compute(commits);

        assertThat(result).extracting(AuthorStats::email)
                .containsExactly("alice@example.com", "bob@example.com");
        assertThat(result).extracting(AuthorStats::commits)
                .containsExactly(2, 1);
    }

    @Test
    void countsDistinctFilesTouched() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 10, 2)),
                Fixtures.fakeCommit("2024-01-09", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 5, 1)));

        AuthorStats alice = calculator.compute(commits).get(0);

        assertThat(alice.filesTouched()).isEqualTo(1);
    }

    @Test
    void mergesAuthorsWithSameNormalizedEmail() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", " Alice@Example.com ",
                        Fixtures.fileChange("Main.java", 10, 2)),
                Fixtures.fakeCommit("2024-01-09", "A. Example", "alice@example.com",
                        Fixtures.fileChange("Service.java", 5, 1)));

        AuthorStats alice = calculator.compute(commits).get(0);

        assertThat(alice.name()).isEqualTo("Alice");
        assertThat(alice.email()).isEqualTo("alice@example.com");
        assertThat(alice.commits()).isEqualTo(2);
        assertThat(alice.filesTouched()).isEqualTo(2);
        assertThat(alice.linesAdded()).isEqualTo(15);
        assertThat(alice.linesDeleted()).isEqualTo(3);
    }

    @Test
    void sumsLinesPerAuthor() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 10, 2),
                        Fixtures.fileChange("Api.java", 5, 3)),
                Fixtures.fakeCommit("2024-01-09", "Bob", "bob@example.com",
                        Fixtures.fileChange("Other.java", 100, 50)),
                Fixtures.fakeCommit("2024-01-10", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 3, 1)));

        AuthorStats alice = calculator.compute(commits).stream()
                .filter(stats -> stats.email().equals("alice@example.com"))
                .findFirst()
                .orElseThrow();

        assertThat(alice.linesAdded()).isEqualTo(18);
        assertThat(alice.linesDeleted()).isEqualTo(6);
    }

    @Test
    void sortsByCommitsDescending() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "Bob", "bob@example.com",
                        Fixtures.fileChange("Api.java", 5, 1)),
                Fixtures.fakeCommit("2024-01-09", "Alice", "alice@example.com",
                        Fixtures.fileChange("Main.java", 10, 2)),
                Fixtures.fakeCommit("2024-01-10", "Alice", "alice@example.com",
                        Fixtures.fileChange("Service.java", 3, 0)));

        List<AuthorStats> result = calculator.compute(commits);

        assertThat(result).extracting(AuthorStats::email)
                .containsExactly("alice@example.com", "bob@example.com");
    }
}
