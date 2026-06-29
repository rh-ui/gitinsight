package com.gitinsight.api.exception;

/**
 * Levée quand un identifiant de job d'analyse est inconnu (expiré ou erroné).
 * Mappée en HTTP 404 par le {@code GlobalExceptionHandler}.
 */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("Aucune analyse trouvée pour cet identifiant : " + jobId);
    }
}
