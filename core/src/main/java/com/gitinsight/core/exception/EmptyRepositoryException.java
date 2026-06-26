package com.gitinsight.core.exception;

import java.io.IOException;

/**
 * Levée quand le dépôt Git existe mais ne contient aucun commit (HEAD non né).
 *
 * <p>
 * Hérite d'{@link IOException} : la couche API la mappe en HTTP 400 (le client a
 * pointé vers un dépôt inexploitable), pas en 500.
 */
public class EmptyRepositoryException extends IOException {

    public EmptyRepositoryException(String message) {
        super(message);
    }

    public EmptyRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
