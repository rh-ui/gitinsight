package com.gitinsight.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gitinsight.core.helpers.GitTestRepo;
import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.Hotspot;
import com.gitinsight.core.model.RepositoryAnalysis;

class AnalysisServiceTest {

    private final AnalysisService service = new AnalysisService();

    @Test
    void analyzesRealRepo(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            GitTestRepo.commitFile(git, repo, "src/Main.java", "class Main {}",
                    "feat: init", "Alice", "alice@example.com", date("2024-01-08"));
            GitTestRepo.commitFile(git, repo, "src/Main.java", "class Main { void run(){} }",
                    "fix: run", "Bob", "bob@example.com", date("2024-01-09"));
            GitTestRepo.commitFile(git, repo, "src/Service.java", "class Service {}",
                    "feat: service", "Alice", "alice@example.com", date("2024-01-15"));
        }

        RepositoryAnalysis analysis = service.analyze(repo, 10);

        assertThat(analysis.meta().totalCommits()).isEqualTo(3);
        assertThat(analysis.meta().firstCommit()).isEqualTo(date("2024-01-08"));
        assertThat(analysis.meta().lastCommit()).isEqualTo(date("2024-01-15"));

        assertThat(analysis.velocity()).hasSize(2);

        assertThat(analysis.authors()).extracting(AuthorStats::email)
                .containsExactly("alice@example.com", "bob@example.com");

        Hotspot top = analysis.hotspots().get(0);
        assertThat(top.path()).isEqualTo("src/Main.java");
        assertThat(top.riskScore()).isEqualTo(4.0);
    }

    @Test
    void computesCommitBoundsFromDatesInsteadOfHistoryOrder(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            GitTestRepo.commitFile(git, repo, "file.txt", "newer author date",
                    "feat: newer author date", "Alice", "alice@example.com", date("2024-01-15"));
            GitTestRepo.commitFile(git, repo, "file.txt", "older author date",
                    "fix: older author date", "Alice", "alice@example.com", date("2024-01-08"));
        }

        RepositoryAnalysis analysis = service.analyze(repo, 10);

        assertThat(analysis.meta().firstCommit()).isEqualTo(date("2024-01-08"));
        assertThat(analysis.meta().lastCommit()).isEqualTo(date("2024-01-15"));
    }

    @Test
    void throwsOnEmptyRepo(@TempDir Path repo) throws Exception {
        Git.init().setDirectory(repo.toFile()).call().close();

        assertThatThrownBy(() -> service.analyze(repo, 10))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("aucun commit");
    }

    private Instant date(String date) {
        return Instant.parse(date + "T10:00:00Z");
    }
}
