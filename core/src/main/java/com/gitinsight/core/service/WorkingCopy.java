package com.gitinsight.core.service;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jgit.util.FileUtils;

/**
 * Référence vers une copie de travail à analyser, qu'elle soit locale ou clonée.
 *
 * <p>
 * Un dépôt distant est cloné dans un dossier temporaire : ce dossier doit être
 * supprimé une fois l'analyse terminée. {@code WorkingCopy} implémente
 * {@link AutoCloseable} pour permettre un nettoyage déterministe via
 * try-with-resources, quel que soit le résultat de l'analyse.
 *
 * <ul>
 * <li>{@link #local(Path)} : dépôt déjà présent sur le disque — jamais supprimé.</li>
 * <li>{@link #temporary(Path)} : clone jetable — supprimé à la fermeture.</li>
 * </ul>
 */
public final class WorkingCopy implements AutoCloseable {

    private final Path path;
    private final boolean temporary;

    private WorkingCopy(Path path, boolean temporary) {
        this.path = path;
        this.temporary = temporary;
    }

    /** Dépôt local existant : la fermeture ne supprime rien. */
    public static WorkingCopy local(Path path) {
        return new WorkingCopy(path, false);
    }

    /** Clone temporaire : la fermeture supprime récursivement le dossier. */
    public static WorkingCopy temporary(Path path) {
        return new WorkingCopy(path, true);
    }

    /** Racine du dépôt à analyser (dossier contenant {@code .git}). */
    public Path path() {
        return path;
    }

    @Override
    public void close() throws IOException {
        if (!temporary) {
            return;
        }
        // FileUtils.delete gère les réessais sur les verrous de fichiers (.pack)
        // que JGit peut conserver sous Windows. IGNORE_ERRORS : un échec de
        // nettoyage du temporaire ne doit pas masquer une analyse réussie.
        FileUtils.delete(path.toFile(),
                FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.IGNORE_ERRORS);
    }
}
