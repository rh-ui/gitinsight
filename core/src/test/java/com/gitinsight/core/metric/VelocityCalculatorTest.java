package com.gitinsight.core.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gitinsight.core.helpers.Fixtures;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.WeeklyVelocity;

class VelocityCalculatorTest {

    private final VelocityCalculator calculator = new VelocityCalculator();

    @Test
    void groupsCommitsByWeek() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "a@x.com", 10, 2),
                Fixtures.fakeCommit("2024-01-09", "b@x.com", 5, 1),
                Fixtures.fakeCommit("2024-01-15", "a@x.com", 3, 0));

        List<WeeklyVelocity> result = calculator.compute(commits);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).commits()).isEqualTo(2);
        assertThat(result.get(1).commits()).isEqualTo(1);
    }

    @Test
    void sumsLinesCorrectly() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "a@x.com", 10, 2),
                Fixtures.fakeCommit("2024-01-09", "a@x.com", 5, 3));

        WeeklyVelocity week = calculator.compute(commits).get(0);

        assertThat(week.linesAdded()).isEqualTo(15);
        assertThat(week.linesDeleted()).isEqualTo(5);
    }

    @Test
    void countsDistinctAuthors() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-08", "a@x.com", 1, 0),
                Fixtures.fakeCommit("2024-01-08", "a@x.com", 1, 0),
                Fixtures.fakeCommit("2024-01-09", "b@x.com", 1, 0));

        WeeklyVelocity week = calculator.compute(commits).get(0);

        assertThat(week.activeAuthors()).isEqualTo(2);
    }

    @Test
    void returnsChronologicalOrder() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-15", "a@x.com", 1, 0),
                Fixtures.fakeCommit("2024-01-08", "a@x.com", 1, 0));

        List<WeeklyVelocity> result = calculator.compute(commits);

        assertThat(result.get(0).weekStart()).isBefore(result.get(1).weekStart());
    }

    @Test
    void fillsEmptyWeeksBetweenActivity() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-01", "a@x.com", 10, 2),
                Fixtures.fakeCommit("2024-01-29", "b@x.com", 5, 1));

        List<WeeklyVelocity> result = calculator.compute(commits);

        assertThat(result).extracting(WeeklyVelocity::weekStart).containsExactly(
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-08"),
                LocalDate.parse("2024-01-15"),
                LocalDate.parse("2024-01-22"),
                LocalDate.parse("2024-01-29"));
        assertThat(result).extracting(WeeklyVelocity::commits).containsExactly(1, 0, 0, 0, 1);
        assertThat(result.get(1).linesAdded()).isZero();
        assertThat(result.get(1).linesDeleted()).isZero();
        assertThat(result.get(1).activeAuthors()).isZero();
    }

    @Test
    void bucketsSundayWithPreviousIsoWeekAndMondayWithNextWeek() {
        List<CommitInfo> commits = List.of(
                Fixtures.fakeCommit("2024-01-14", "a@x.com", 1, 0),
                Fixtures.fakeCommit("2024-01-15", "b@x.com", 1, 0));

        List<WeeklyVelocity> result = calculator.compute(commits);

        assertThat(result).extracting(WeeklyVelocity::weekStart).containsExactly(
                LocalDate.parse("2024-01-08"),
                LocalDate.parse("2024-01-15"));
        assertThat(result).extracting(WeeklyVelocity::commits).containsExactly(1, 1);
    }
}
