package com.gitinsight.core.helpers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;

public final class GitTestRepo {

    private GitTestRepo() {
    }

    public static void commitFile(Git git, Path repo, String relPath, String content, String message)
            throws Exception {
        commitFile(git, repo, relPath, content, message, "Alice", "alice@example.com", Instant.now());
    }

    public static void commitFile(Git git, Path repo, String relPath, String content, String message,
            String name, String email, Instant date) throws Exception {
        Path file = repo.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        git.add().addFilepattern(relPath).call();
        commitStaged(git, message, name, email, date);
    }

    public static void commitStaged(Git git, String message) throws Exception {
        commitStaged(git, message, "Alice", "alice@example.com", Instant.now());
    }

    public static void commitStaged(Git git, String message, String name, String email, Instant date) throws Exception {
        PersonIdent ident = new PersonIdent(name, email, date, ZoneOffset.UTC);
        git.commit()
                .setMessage(message)
                .setAuthor(ident)
                .setCommitter(ident)
                .call();
    }
}
