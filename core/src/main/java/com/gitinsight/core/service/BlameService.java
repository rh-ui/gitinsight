package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;

import com.gitinsight.core.exception.EmptyRepositoryException;
import com.gitinsight.core.exception.NotAGitRepositoryException;
import com.gitinsight.core.model.FileOwnership;

/**
 * Calcule la propriété (ownership) des lignes des fichiers à HEAD via git blame.
 *
 * Pour chaque chemin candidat, on attribue chaque ligne actuelle à l'auteur qui
 * l'a écrite en dernier, puis on en déduit l'auteur dominant et sa part. C'est la
 * base du « bus factor » : un fichier détenu à ~100 % par une personne est fragile.
 *
 * <p>
 * <b>Perf :</b> blamer un fichier coûte O(historique du fichier). On ne blâme donc
 * jamais l'arbre entier : l'appelant fournit un ensemble borné de candidats (les
 * top-N fichiers les plus modifiés encore présents au HEAD). Optimisation future
 * possible : paralléliser les blames.
 */
public class BlameService {

    /**
     * Calcule l'ownership pour chaque chemin candidat présent au HEAD.
     *
     * @param repoPath       racine d'un dépôt Git.
     * @param candidatePaths chemins (relatifs au dépôt) à blâmer ; borné par l'appelant.
     * @return un {@link FileOwnership} par fichier réellement présent et non vide ;
     *         les chemins disparus/vides sont silencieusement ignorés.
     * @throws IOException si le dépôt est introuvable, vide ou illisible.
     */
    public List<FileOwnership> computeOwnership(Path repoPath, Collection<String> candidatePaths)
            throws IOException {
        try (Git git = Git.open(repoPath.toFile())) {
            ObjectId headId = git.getRepository().resolve("HEAD");
            if (headId == null) {
                throw new EmptyRepositoryException("Le depot ne contient aucun commit : " + repoPath);
            }

            List<FileOwnership> result = new ArrayList<>();
            for (String path : candidatePaths) {
                FileOwnership ownership = blameFile(git, headId, path);
                if (ownership != null) {
                    result.add(ownership);
                }
            }
            return List.copyOf(result);
        } catch (RepositoryNotFoundException e) {
            throw new NotAGitRepositoryException("Aucun dépôt Git trouvé à ce chemin : " + repoPath, e);
        } catch (GitAPIException e) {
            throw new IOException("Échec du calcul du bus factor (git blame) : " + repoPath, e);
        }
    }

    /** Blâme un fichier ; retourne {@code null} s'il a disparu du HEAD ou est vide. */
    private FileOwnership blameFile(Git git, ObjectId headId, String path)
            throws GitAPIException {
        BlameResult blame = git.blame()
                .setFilePath(path)
                .setStartCommit(headId)
                .setFollowFileRenames(true)
                .setTextComparator(RawTextComparator.WS_IGNORE_ALL)
                .call();

        if (blame == null) {
            return null; // fichier absent au HEAD -> on skip, pas d'erreur
        }

        int totalLines = blame.getResultContents().size();
        if (totalLines == 0) {
            return null; // fichier vide -> éviter ownership = 0/0 = NaN
        }

        Map<String, Integer> linesByEmail = new HashMap<>();
        Map<String, String> nameByEmail = new HashMap<>();
        for (int i = 0; i < totalLines; i++) {
            PersonIdent author = blame.getSourceAuthor(i);
            if (author == null) {
                continue;
            }
            String email = author.getEmailAddress();
            linesByEmail.merge(email, 1, Integer::sum);
            nameByEmail.putIfAbsent(email, author.getName());
        }

        Map.Entry<String, Integer> top = linesByEmail.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (top == null) {
            return null; // aucune ligne attribuable
        }

        String topEmail = top.getKey();
        int topLines = top.getValue();
        return new FileOwnership(
                path,
                nameByEmail.get(topEmail),
                topEmail,
                topLines,
                totalLines,
                (double) topLines / totalLines);
    }
}