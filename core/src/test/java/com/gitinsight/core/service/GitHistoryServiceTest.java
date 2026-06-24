package com.gitinsight.core.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gitinsight.core.model.CommitInfo;

/**
 * Tests hermétiques : chaque test crée un dépôt Git jetable ({@code @TempDir})
 * avec des données connues, ce qui permet d'asserter des valeurs exactes
 * indépendamment de la machine ou de l'état du repo GitInsight.
 */
class GitHistoryServiceTest {

    private final GitHistoryService service = new GitHistoryService();

    @Test
    void extractsCommitMetadata(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            commit(git, repo, "README.md", "hello", "feat: initial commit");

            List<CommitInfo> history = service.getHistory(repo, 10);

            assertThat(history).hasSize(1);
            CommitInfo c = history.get(0);
            assertThat(c.hash()).hasSize(40);
            assertThat(c.authorName()).isEqualTo("Alice");
            assertThat(c.authorEmail()).isEqualTo("alice@example.com");
            assertThat(c.message()).isEqualTo("feat: initial commit");
            assertThat(c.date()).isNotNull();
        }
    }

    @Test
    void capturesFilesAddedByRootCommit(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            commit(git, repo, "src/Main.java", "class Main {}", "feat: root");

            List<CommitInfo> history = service.getHistory(repo, 10);

            // Régression : le commit racine n'était pas diffé → ses fichiers ajoutés
            // étaient perdus, ce qui aurait sous-compté hotspots et bus factor.
            assertThat(history.get(0).changedFiles()).containsExactly("src/Main.java");
        }
    }

    @Test
    void capturesDeletedFilePath(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            commit(git, repo, "todo.txt", "a", "feat: add todo");
            git.rm().addFilepattern("todo.txt").call();
            commitStaged(git, "chore: remove todo");

            List<CommitInfo> history = service.getHistory(repo, 10);

            // history.get(0) = commit de suppression (le plus récent).
            // Régression : on émettait "/dev/null" au lieu du chemin réel.
            assertThat(history.get(0).changedFiles()).containsExactly("todo.txt");
        }
    }

    @Test
    void respectsLimit(@TempDir Path repo) throws Exception {
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            commit(git, repo, "f1", "1", "c1");
            commit(git, repo, "f2", "2", "c2");
            commit(git, repo, "f3", "3", "c3");

            assertThat(service.getHistory(repo, 2)).hasSize(2);
        }
    }

    @Test
    void rejectsNonPositiveLimit(@TempDir Path repo) {
        assertThatThrownBy(() -> service.getHistory(repo, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // --- helpers ---

    private void commit(Git git, Path repo, String relPath, String content, String message)
            throws Exception {
        Path file = repo.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        git.add().addFilepattern(relPath).call();
        commitStaged(git, message);
    }

    private void commitStaged(Git git, String message) throws Exception {
        git.commit()
            .setMessage(message)
            .setAuthor("Alice", "alice@example.com")
            .setCommitter("Alice", "alice@example.com")
            .call();
    }
}
