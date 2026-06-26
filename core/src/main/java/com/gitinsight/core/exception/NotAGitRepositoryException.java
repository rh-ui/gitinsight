package com.gitinsight.core.exception;

import java.io.IOException;

/**
 * Levée quand le chemin fourni ne pointe pas vers un dépôt Git valide
 * (chemin inexistant, fichier au lieu d'un dossier, ou dossier sans {@code .git}).
 *
 * <p>
 * Hérite d'{@link IOException} pour rester compatible avec les signatures
 * {@code throws IOException} de la couche service ; la couche API la mappe en
 * réponse HTTP 400 (faute du client) plutôt qu'en 500.
 */
public class NotAGitRepositoryException extends IOException {

    public NotAGitRepositoryException(String message) {
        super(message);
    }

    public NotAGitRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
