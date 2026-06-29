package com.gitinsight.core.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gitinsight.core.exception.RemoteRepositoryException;
import com.gitinsight.core.helpers.GitTestRepo;
import com.gitinsight.core.model.CommitInfo;

class RepositoryResolverTest {

    private final RepositoryResolver resolver = new RepositoryResolver();

    @Test
    void detectsRemoteUrls() {
        assertThat(RepositoryResolver.isRemoteUrl("https://github.com/rh-ui/CHATBOT-FSO.git")).isTrue();
        assertThat(RepositoryResolver.isRemoteUrl("http://example.com/team/repo")).isTrue();
        assertThat(RepositoryResolver.isRemoteUrl("/home/user/projet")).isFalse();
        assertThat(RepositoryResolver.isRemoteUrl("D:\\GitInsight")).isFalse();
        assertThat(RepositoryResolver.isRemoteUrl(".")).isFalse();
        assertThat(RepositoryResolver.isRemoteUrl(null)).isFalse();
    }

    @Test
    void localPathResolvesWithoutCloning(@TempDir Path repo) throws Exception {
        try (WorkingCopy wc = resolver.resolve(repo.toString())) {
            assertThat(wc.path()).isEqualTo(repo);
        }
        assertThat(Files.exists(repo)).isTrue(); // un dépôt local n'est jamais supprimé
    }

    @Test
    void rejectsUnsupportedTransport() {
        // Schéma SSH : détecté comme distant (.git) mais refusé faute de transport.
        assertThatThrownBy(() -> resolver.resolve("git@github.com:rh-ui/CHATBOT-FSO.git"))
                .isInstanceOf(RemoteRepositoryException.class)
                .hasMessageContaining("non supportée");
    }

    @Test
    void clonesRemoteRepoThenCleansUpTempDir(@TempDir Path tmp) throws Exception {
        // 1. Un dépôt "origin" avec un commit.
        Path origin = tmp.resolve("origin");
        Files.createDirectories(origin);
        try (Git git = Git.init().setDirectory(origin.toFile()).call()) {
            GitTestRepo.commitFile(git, origin, "README.md", "hello", "feat: init");
        }
        // 2. Un "remote" bare que le resolver clonera (via file:// — hors réseau).
        Path remote = tmp.resolve("remote.git");
        Git.cloneRepository()
                .setBare(true)
                .setURI(origin.toUri().toString())
                .setDirectory(remote.toFile())
                .call()
                .close();

        // 3. Le clone atterrit dans un dossier temporaire, supprimé à la fermeture.
        Path cloned;
        try (WorkingCopy wc = resolver.cloneToTempDir(remote.toUri().toString())) {
            cloned = wc.path();
            assertThat(Files.exists(cloned.resolve(".git"))).isTrue();
            List<CommitInfo> history = new GitHistoryService().getHistory(cloned);
            assertThat(history).hasSize(1);
        }
        assertThat(Files.exists(cloned)).isFalse(); // clone temporaire nettoyé
    }
}
