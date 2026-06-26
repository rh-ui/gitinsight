package com.gitinsight.api.web.model;

/**
 * Corps d'erreur uniforme renvoyé par {@code GlobalExceptionHandler}.
 *
 * @param status code HTTP (ex. 400, 500)
 * @param error  libellé court du statut (ex. "Bad Request")
 * @param message explication lisible destinée au client
 */
public record ErrorResponse(int status, String error, String message) {
}
