package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import com.gitinsight.core.model.CommitInfo;

public class GitHistoryService {

    public List<CommitInfo> getHistory(Path repoPath, int limit) throws IOException {
        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(repoPath.resolve(".git").toFile())
                .build();
             Git git = new Git(repo)) {

            Iterable<RevCommit> commits = git.log().setMaxCount(limit).call();
            List<CommitInfo> result = new ArrayList<>();

            for (RevCommit commit : commits) {
                List<String> files = getChangedFiles(repo, commit);
                result.add(new CommitInfo(
                    commit.getName(),
                    commit.getAuthorIdent().getName(),
                    commit.getAuthorIdent().getEmailAddress(),
                    commit.getAuthorIdent().getWhenAsInstant(),
                    commit.getShortMessage(),
                    files
                ));
            }
            return result;
        } catch (GitAPIException e) {
            throw new IOException("Erreur JGit", e);
        }
    }

    private List<String> getChangedFiles(Repository repo, RevCommit commit) throws IOException {
        if (commit.getParentCount() == 0) return List.of();

        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, commit.getParent(0).getTree());
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, commit.getTree());

            try (Git git = new Git(repo)) {
                return git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .call()
                    .stream()
                    .map(entry -> entry.getNewPath())
                    .toList();
            } catch (GitAPIException e) {
                return List.of();
            }
        }
    }
}
