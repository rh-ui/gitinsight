package com.gitinsight.core.metric;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.Test;

import com.gitinsight.core.helpers.Fixtures;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileCoupling;

class CouplingCalculatorTest {

    private final CouplingCalculator calculator = new CouplingCalculator();

    @Test
    void computesJaccardForCoupledFiles() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-01", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-02", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-03", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-04", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0)));

        List<FileCoupling> result = calculator.compute(commits, 10, 50);

        assertThat(result).hasSize(1).first().satisfies(fc -> {
            assertThat(fc.fileA()).isEqualTo("A.java");
            assertThat(fc.fileB()).isEqualTo("B.java");
            assertThat(fc.coChanges()).isEqualTo(3);
            assertThat(fc.changesA()).isEqualTo(4);
            assertThat(fc.changesB()).isEqualTo(3);
            assertThat(fc.couplingScore()).isEqualTo(0.75); // 3 / (4 + 3 - 3)
        });
    }

    @Test
    void sortsByScoreDescThenAppliesTopN() {
        List<CommitInfo> commits = List.of(
                // (A,B) co-change x3, jamais seuls → Jaccard 3/3 = 1.0
                Fixtures.fakeCommit("2024-01-01", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-02", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-03", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)),
                // (C,D) co-change x2 + chacun seul une fois → Jaccard 2/(3+3-2) = 0.5
                Fixtures.fakeCommit("2024-01-04", "Alice", "a@x.com",
                        Fixtures.fileChange("C.java", 1, 0), Fixtures.fileChange("D.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-05", "Alice", "a@x.com",
                        Fixtures.fileChange("C.java", 1, 0), Fixtures.fileChange("D.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-06", "Alice", "a@x.com",
                        Fixtures.fileChange("C.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-07", "Alice", "a@x.com",
                        Fixtures.fileChange("D.java", 1, 0)));

        assertThat(calculator.compute(commits, 10, 50))
                .extracting(FileCoupling::fileA, FileCoupling::fileB)
                .containsExactly(tuple("A.java", "B.java"), tuple("C.java", "D.java"));

        assertThat(calculator.compute(commits, 1, 50))
                .extracting(FileCoupling::fileA)
                .containsExactly("A.java");
    }

    @Test
    void skipsPairingForCommitsExceedingMaxFiles() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-01", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0),
                        Fixtures.fileChange("B.java", 1, 0),
                        Fixtures.fileChange("C.java", 1, 0)),
                Fixtures.fakeCommit("2024-01-02", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0),
                        Fixtures.fileChange("B.java", 1, 0),
                        Fixtures.fileChange("C.java", 1, 0)));

        // 3 fichiers > maxFilesPerCommit=2 → aucune paire générée
        assertThat(calculator.compute(commits, 10, 2)).isEmpty();
        // borne relevée → les paires réapparaissent
        assertThat(calculator.compute(commits, 10, 5)).isNotEmpty();
    }

    @Test
    void filtersPairsWithSingleCoChange() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-01", "Alice", "a@x.com",
                        Fixtures.fileChange("A.java", 1, 0), Fixtures.fileChange("B.java", 1, 0)));

        // co-change unique → filtré (coChanges < 2)
        assertThat(calculator.compute(commits, 10, 50)).isEmpty();
    }
}