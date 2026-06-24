package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import com.gitinsight.core.model.ChangeType;
import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;

/**
 * Lit l'historique d'un dépôt Git local et le projette en {@link CommitInfo}.
 *
 * <p>
 * Pour chaque commit, on calcule la liste des fichiers modifiés en diffant
 * l'arbre du commit contre celui de son premier parent. Pour le commit racine
 * (sans parent), on diffe contre un arbre vide afin de capturer les fichiers
 * initialement ajoutés.
 */
public class GitHistoryService {

    /**
     * Retourne les {@code limit} commits les plus récents accessibles depuis HEAD.
     *
     * @param repoPath racine d'un dépôt Git (le dossier contenant {@code .git}).
     * @param limit    nombre maximum de commits à remonter ; doit être &gt; 0.
     * @throws IllegalArgumentException si {@code limit <= 0}.
     * @throws IOException              si le dépôt est introuvable ou illisible.
     */
    public List<CommitInfo> getHistory(Path repoPath, int limit) throws IOException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit doit être > 0, reçu : " + limit);
        }

        // Git.open() remonte jusqu'au .git et gère worktrees/submodules
        // (où .git est un fichier) ainsi que les dépôts bare.
        // Un seul ObjectReader et un seul DiffFormatter sont réutilisés sur tout
        // le parcours — éviter de les recréer par commit est clé sur gros repos.
        try (Git git = Git.open(repoPath.toFile());
                ObjectReader reader = git.getRepository().newObjectReader();
                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDetectRenames(true);

            Iterable<RevCommit> commits = git.log().setMaxCount(limit).call();
            List<CommitInfo> result = new ArrayList<>();

            for (RevCommit commit : commits) {
                List<FileChange> changes = changedFiles(reader, diffFormatter, commit);
                result.add(new CommitInfo(
                        commit.getName(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        commit.getAuthorIdent().getWhenAsInstant(),
                        commit.getShortMessage(),
                        changes));
            }
            return result;
        } catch (GitAPIException e) {
            throw new IOException("Échec de la lecture de l'historique Git : " + repoPath, e);
        }
    }

    private List<FileChange> changedFiles(ObjectReader reader, DiffFormatter diffFormatter, RevCommit commit)
            throws IOException {

        AbstractTreeIterator parentTree = commit.getParentCount() > 0
                ? treeIterator(reader, commit.getParent(0).getTree())
                : new EmptyTreeIterator();
        AbstractTreeIterator currentTree = treeIterator(reader, commit.getTree());

        List<DiffEntry> diffs = diffFormatter.scan(parentTree, currentTree);
        List<FileChange> files = new ArrayList<>(diffs.size());

        for (DiffEntry entry : diffs) {
            int linesAdded = 0;
            int linesDeleted = 0;

            for (var edit : diffFormatter.toFileHeader(entry).toEditList()) {
                linesAdded += edit.getEndB() - edit.getBeginB();
                linesDeleted += edit.getEndA() - edit.getBeginA();
            }

            files.add(new FileChange(
                    pathOf(entry),
                    mapChangeType(entry.getChangeType()),
                    linesAdded,
                    linesDeleted));
        }
        return List.copyOf(files);
    }

    private ChangeType mapChangeType(DiffEntry.ChangeType jgitType) {
        return switch (jgitType) {
            case ADD -> ChangeType.ADD;
            case MODIFY -> ChangeType.MODIFY;
            case DELETE -> ChangeType.DELETE;
            case RENAME -> ChangeType.RENAME;
            case COPY -> ChangeType.MODIFY;
        };
    }

    public List<CommitInfo> getHistory(Path repoPath) throws IOException {
        try (Git git = Git.open(repoPath.toFile());
                ObjectReader reader = git.getRepository().newObjectReader();
                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDetectRenames(true);

            Iterable<RevCommit> commits = git.log().call();
            List<CommitInfo> result = new ArrayList<>();

            for (RevCommit commit : commits) {
                List<FileChange> changes = changedFiles(reader, diffFormatter, commit);
                result.add(new CommitInfo(
                        commit.getName(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        commit.getAuthorIdent().getWhenAsInstant(),
                        commit.getShortMessage(),
                        changes));
            }
            return result;
        } catch (GitAPIException e) {
            throw new IOException("Échec de la lecture de l'historique Git : " + repoPath, e);
        }
    }

    private AbstractTreeIterator treeIterator(ObjectReader reader, RevTree tree) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        parser.reset(reader, tree);
        return parser;
    }

    private String pathOf(DiffEntry entry) {
        return entry.getChangeType() == DiffEntry.ChangeType.DELETE
                ? entry.getOldPath()
                : entry.getNewPath();
    }
}
