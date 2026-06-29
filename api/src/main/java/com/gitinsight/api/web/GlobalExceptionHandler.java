package com.gitinsight.api.web;

import java.io.IOException;

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
import com.gitinsight.core.exception.RemoteRepositoryException;

/**
 * Traduit les exceptions en réponses HTTP cohérentes, centralisées hors du
 * contrôleur. On distingue clairement les fautes du client (400, avec message)
 * des erreurs serveur (500), au lieu de tout remonter en 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Faute du client sur le dépôt fourni : chemin invalide, pas un dépôt Git,
     * dépôt vide, ou URL distante invalide/injoignable (échec du clonage).
     */
    @ExceptionHandler({ NotAGitRepositoryException.class, EmptyRepositoryException.class,
            RemoteRepositoryException.class })
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

    /** Chemin syntaxiquement invalide (InvalidPathException) et autres args illégaux. */
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
