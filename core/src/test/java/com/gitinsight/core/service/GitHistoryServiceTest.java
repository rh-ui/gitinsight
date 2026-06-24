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
        // On pointe sur le repo GitInsight lui-même
        Path repoPath = Path.of(System.getProperty("user.dir"))
                            .getParent(); // remonte à la racine du projet

        GitHistoryService service = new GitHistoryService();
        List<CommitInfo> commits = service.getHistory(repoPath, 10);

        assertThat(commits).isNotEmpty();
        assertThat(commits.get(0).hash()).hasSize(40);
        assertThat(commits.get(0).authorName()).isNotBlank();

        // Affiche pour vérifier visuellement
        commits.forEach(c -> System.out.printf("[%s] %s — %s%n",
            c.date(), c.authorName(), c.message()));
    }
}
