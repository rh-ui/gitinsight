package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.util.FileUtils;

import com.gitinsight.core.exception.RemoteRepositoryException;

/**
 * Transforme une source de dépôt (chemin local OU URL distante) en une
 * {@link WorkingCopy} analysable localement.
 *
 * <p>
 * Le reste du moteur ne sait travailler que sur un dossier local. Plutôt que de
 * propager la notion d'« URL » dans tout le pipeline (history, blame, couplage),
 * on centralise ici la résolution : une URL distante est clonée dans un dossier
 * temporaire, et l'analyse réutilise tel quel le chemin obtenu.
 *
 * <p>
 * <b>Clone complet, volontairement :</b> les métriques (vélocité, bus factor,
 * couplage) ont besoin de tout l'historique. Un clone superficiel
 * ({@code --depth}) les fausserait silencieusement.
 *
 * <p>
 * <b>Sécurité :</b> l'URL provient du client. Seul le transport HTTP(S) embarqué
 * par JGit est accepté ; les autres schémas (file, ftp, ssh…) sont refusés pour
 * limiter la surface SSRF/accès disque. Restreindre à une liste d'hôtes
 * (github.com, gitlab.com…) est une amélioration possible ultérieurement.
 */
public class RepositoryResolver {

    /** Plafond (secondes) des opérations réseau du clone : évite un blocage infini. */
    private static final int CLONE_TIMEOUT_SECONDS = 120;

    /**
     * Résout une source en copie de travail locale.
     *
     * @param source chemin d'un dépôt local, ou URL HTTP(S) d'un dépôt distant.
     * @return une {@link WorkingCopy} ; pour une URL, un clone temporaire que
     *         l'appelant DOIT fermer (try-with-resources) pour le supprimer.
     * @throws RemoteRepositoryException si l'URL est rejetée ou si le clonage échoue.
     * @throws IOException               si le dossier temporaire ne peut être créé.
     */
    public WorkingCopy resolve(String source) throws IOException {
        if (!isRemoteUrl(source)) {
            return WorkingCopy.local(Path.of(source));
        }

        String url = source.trim();
        if (!isSupportedRemoteUrl(url)) {
            throw new RemoteRepositoryException(
                    "URL de dépôt non supportée (seul HTTP/HTTPS est accepté) : " + url);
        }
        return cloneToTempDir(url);
    }

    /**
     * Clone {@code uri} dans un dossier temporaire jetable et l'emballe dans une
     * {@link WorkingCopy} temporaire (supprimée à la fermeture).
     *
     * <p>
     * Visibilité paquet (non {@code private}) : permet aux tests de vérifier le
     * clonage et le nettoyage hors-ligne en clonant depuis un dépôt local
     * ({@code file://}), sans dépendre du réseau ni contourner la validation de
     * schéma faite par {@link #resolve(String)}.
     */
    WorkingCopy cloneToTempDir(String uri) throws IOException {
        Path tmp = Files.createTempDirectory("gitinsight-");
        try (Git git = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(tmp.toFile())
                .setTimeout(CLONE_TIMEOUT_SECONDS)
                .call()) {
            // Le clone a réussi ; le try-with-resources ferme le handle Git pour
            // libérer les fichiers avant toute suppression ultérieure du dossier.
            return WorkingCopy.temporary(tmp);
        } catch (GitAPIException e) {
            FileUtils.delete(tmp.toFile(),
                    FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.IGNORE_ERRORS);
            throw new RemoteRepositoryException(
                    "Échec du clonage du dépôt distant : " + uri + " — " + rootCauseMessage(e), e);
        }
    }

    /** Message de la cause la plus profonde (la vraie raison : 401, 404, DNS…). */
    private static String rootCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return (msg == null || msg.isBlank()) ? cause.getClass().getSimpleName() : msg;
    }

    /**
     * Heuristique : la source désigne-t-elle un dépôt distant à cloner ?
     *
     * <p>
     * On reconnaît les URL HTTP(S) et tout ce qui se termine par {@code .git}
     * (convention universelle des URL de clone). Tout le reste est traité comme
     * un chemin local.
     */
    public static boolean isRemoteUrl(String source) {
        if (source == null) {
            return false;
        }
        String s = source.trim();
        return s.startsWith("http://")
                || s.startsWith("https://")
                || s.endsWith(".git");
    }

    /** Transport réellement supporté par JGit sans dépendance additionnelle. */
    private static boolean isSupportedRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
