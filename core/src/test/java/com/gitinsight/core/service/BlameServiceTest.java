package com.gitinsight.core.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gitinsight.core.helpers.GitTestRepo;
import com.gitinsight.core.model.FileOwnership;

class BlameServiceTest {

    private final BlameService service = new BlameService();

    @Test
    void singleAuthorOwnsFileEntirely(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            GitTestRepo.commitFile(git, repo, "Main.java", "line1\nline2\n", "feat: init");
            GitTestRepo.commitFile(git, repo, "Main.java", "line1\nline2\nline3\n", "feat: grow");

            List<FileOwnership> result = service.computeOwnership(repo, List.of("Main.java"));

            assertThat(result).hasSize(1).first().satisfies(fo -> {
                assertThat(fo.path()).isEqualTo("Main.java");
                assertThat(fo.topAuthor()).isEqualTo("Alice");
                assertThat(fo.topAuthorEmail()).isEqualTo("alice@example.com");
                assertThat(fo.ownership()).isEqualTo(1.0);
                assertThat(fo.topAuthorLines()).isEqualTo(fo.totalLines());
            });
        }
    }

    @Test
    void splitOwnershipWhenBobRewritesHalf(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            GitTestRepo.commitFile(git, repo, "Shared.java",
                    "alpha\nbeta\ngamma\ndelta\n", "feat: alice writes",
                    "Alice", "alice@example.com", Instant.now());
            GitTestRepo.commitFile(git, repo, "Shared.java",
                    "alpha\nbeta\nGAMMA\nDELTA\n", "refactor: bob rewrites half",
                    "Bob", "bob@example.com", Instant.now());

            List<FileOwnership> result = service.computeOwnership(repo, List.of("Shared.java"));

            assertThat(result).hasSize(1).first().satisfies(fo -> {
                assertThat(fo.totalLines()).isEqualTo(4);
                assertThat(fo.topAuthorLines()).isEqualTo(2);
                assertThat(fo.ownership()).isEqualTo(0.5);
            });
        }
    }

    @Test
    void skipsFileDeletedAtHead(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            GitTestRepo.commitFile(git, repo, "Doomed.java", "a\nb\n", "feat: add");
            GitTestRepo.commitFile(git, repo, "Keep.java", "x\n", "feat: keep");
            git.rm().addFilepattern("Doomed.java").call();
            GitTestRepo.commitStaged(git, "chore: remove Doomed");

            List<FileOwnership> result =
                    service.computeOwnership(repo, List.of("Doomed.java", "Keep.java"));

            assertThat(result).extracting(FileOwnership::path).containsExactly("Keep.java");
        }
    }

    @Test
    void ignoresNonexistentCandidatePath(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            GitTestRepo.commitFile(git, repo, "Real.java", "a\n", "feat: add");

            List<FileOwnership> result =
                    service.computeOwnership(repo, List.of("ghost/Nope.java"));

            assertThat(result).isEmpty();
        }
    }
}