package com.gitinsight.api.web;

import java.io.IOException;
import java.nio.file.InvalidPathException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gitinsight.api.exception.JobNotFoundException;
import com.gitinsight.api.web.model.ErrorResponse;
import com.gitinsight.core.exception.EmptyRepositoryException;
import com.gitinsight.core.exception.NotAGitRepositoryException;

/**
 * Traduit les exceptions en réponses HTTP cohérentes, centralisées hors du
 * contrôleur. On distingue clairement les fautes du client (400, avec message)
 * des erreurs serveur (500), au lieu de tout remonter en 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Chemin invalide / pas un dépôt Git / dépôt sans commit → faute du client. */
    @ExceptionHandler({ NotAGitRepositoryException.class, EmptyRepositoryException.class })
    public ResponseEntity<ErrorResponse> handleInvalidRepository(IOException e) {
        return badRequest(e.getMessage());
    }

    /** Identifiant de job d'analyse inconnu (expiré ou erroné). */
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(JobNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", e.getMessage()));
    }

    /** Échec de validation Bean Validation (@NotBlank, @Positive, ...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .reduce((a, b) -> a + " ; " + b)
                .orElse("Requête invalide.");
        return badRequest(message);
    }

    /** Corps JSON absent, mal formé, ou non parsable (ex. apostrophes du shell). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest("Corps de requête JSON absent ou mal formé.");
    }

    /**
     * Chaîne non convertible en chemin de fichier — en pratique une URL collée
     * dans le champ « dépôt » (le « : » du schéma est illégal dans un chemin
     * Windows). On explique la contrainte plutôt que de renvoyer le
     * « Illegal char &lt;:&gt; at index 5 » de la JVM, incompréhensible côté UI.
     *
     * <p>
     * Plus spécifique que {@link #handleIllegalArgument} dont
     * {@code InvalidPathException} hérite : Spring retient le handler le plus
     * proche dans la hiérarchie.
     */
    @ExceptionHandler(InvalidPathException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPath(InvalidPathException e) {
        return badRequest("Chemin de dépôt invalide : « " + e.getInput()
                + " ». GitInsight analyse uniquement des dépôts déjà présents sur le disque.");
    }

    /** Autres arguments illégaux remontés depuis le core. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return badRequest(e.getMessage());
    }

    /** Vraie erreur d'I/O lors de la lecture du dépôt → erreur serveur. */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIo(IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "Erreur de lecture du dépôt Git."));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Bad Request", message));
    }
}
