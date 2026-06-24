package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.gitinsight.core.model.CommitInfo;

class GitHistoryServiceTest {

    @Test
    void shouldExtractCommitsFromCurrentRepo() throws IOException {
        Path repoPath = Path.of(System.getProperty("user.dir")).getParent(); // recupere le chemin dyal lproject à analyser , user.dir : katjib dossier courant depuis lequel le test est exécuté, getParent() : remonte d'un nieau

        GitHistoryService service = new GitHistoryService(); // create service metier 

        /**
         * Lis l’historique Git du dépôt repoPath
         * Retourne les 10 derniers commits
         */
        List<CommitInfo> commits = service.getHistory(repoPath, 10);

        assertThat(commits).isNotEmpty();
        assertThat(commits.get(0).hash()).hasSize(40);
        assertThat(commits.get(0).authorName()).isNotBlank();

        commits.forEach(c -> System.out.printf("[%s] %s — %s%n",
                c.date(), c.authorName(), c.message()));
    }
}
