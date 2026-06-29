package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gitinsight.core.helpers.GitTestRepo;
import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.FileCoupling;
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

    @Test
    void enrichesAnalysisWithBusFactorAndCoupling(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            // commit 1 : A et B ajoutés ENSEMBLE (Alice)
            write(repo, "A.java", "a1\na2\na3\n");
            write(repo, "B.java", "b1\nb2\n");
            git.add().addFilepattern(".").call();
            GitTestRepo.commitStaged(git, "feat: add A and B", "Alice", "alice@example.com", date("2024-01-01"));

            // commit 2 : A et B modifiés ENSEMBLE (Alice)
            write(repo, "A.java", "a1\na2\na3\na4\n");
            write(repo, "B.java", "b1\nb2\nb3\n");
            git.add().addFilepattern(".").call();
            GitTestRepo.commitStaged(git, "feat: grow A and B", "Alice", "alice@example.com", date("2024-01-02"));

            // commit 3 : A seul (Bob)
            write(repo, "A.java", "a1\na2\na3\na4\na5\n");
            git.add().addFilepattern(".").call();
            GitTestRepo.commitStaged(git, "fix: tweak A", "Bob", "bob@example.com", date("2024-01-03"));
        }

        RepositoryAnalysis analysis = service.analyze(repo, 10, 30);

        // Bus factor : non vide, ownership ∈ [0,1]
        assertThat(analysis.busFactor()).isNotEmpty();
        assertThat(analysis.busFactor())
                .allSatisfy(fo -> assertThat(fo.ownership()).isBetween(0.0, 1.0));

        // Couplage : A et B couplés (co-change x2), trié par score desc
        assertThat(analysis.coupling()).isNotEmpty();
        FileCoupling top = analysis.coupling().get(0);
        assertThat(top.fileA()).isEqualTo("A.java");
        assertThat(top.fileB()).isEqualTo("B.java");
        assertThat(top.coChanges()).isEqualTo(2);
        assertThat(top.couplingScore()).isBetween(0.0, 1.0);
        assertThat(analysis.coupling())
                .isSortedAccordingTo(Comparator.comparingDouble(FileCoupling::couplingScore).reversed());
    }

    private static void write(Path repo, String relPath, String content) throws Exception {
        Files.writeString(repo.resolve(relPath), content);
    }

    private Instant date(String date) {

        return Instant.parse(date + "T10:00:00Z");
    }
}
