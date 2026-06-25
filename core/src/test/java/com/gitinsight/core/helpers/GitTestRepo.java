package com.gitinsight.core.helpers;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;

public final class GitTestRepo {

    private GitTestRepo() {
    }

    public static void commitFile(Git git, Path repo, String relPath, String content, String message)
            throws Exception {
        Path file = repo.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        git.add().addFilepattern(relPath).call();
        commitStaged(git, message);
    }

    public static void commitStaged(Git git, String message) throws Exception {
        git.commit()
                .setMessage(message)
                .setAuthor("Alice", "alice@example.com")
                .setCommitter("Alice", "alice@example.com")
                .call();
    }
}
