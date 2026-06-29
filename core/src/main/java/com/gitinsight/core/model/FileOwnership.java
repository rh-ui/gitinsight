package com.gitinsight.core.model;

/**
 * Propriété (ownership) des lignes d'un fichier à HEAD, calculée par git blame.
 *
 * {@code ownership = topAuthorLines / totalLines} (0..1). Une valeur proche de
 * 1
 * signifie qu'un seul auteur détient quasiment tout le fichier : le « bus
 * factor »
 * de ce fichier vaut 1 (si cette personne part, plus personne ne le connaît).
 *
 * @param path           chemin du fichier (relatif à la racine du dépôt)
 * @param topAuthor      nom de l'auteur détenant le plus de lignes
 * @param topAuthorEmail email de cet auteur
 * @param topAuthorLines nombre de lignes détenues par cet auteur
 * @param totalLines     nombre total de lignes du fichier à HEAD
 * @param ownership      part des lignes détenues par l'auteur dominant (0..1)
 */
public record FileOwnership(
        String path,
        String topAuthor,
        String topAuthorEmail,
        int topAuthorLines,
        int totalLines,
        double ownership) {
}