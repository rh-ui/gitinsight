package com.gitinsight.core.model;

/**
 * Couplage temporel entre deux fichiers : à quelle fréquence ils changent
 * ensemble dans l'historique.
 *
 * La paire est non ordonnée mais stockée sous forme canonique
 * ({@code fileA < fileB} lexicographiquement) pour ne jamais compter (a,b) et
 * (b,a) séparément.
 *
 * {@code couplingScore} est l'indice de Jaccard :
 * {@code coChanges / (changesA + changesB - coChanges)} (0..1). Contrairement à
 * un simple co-compte, il est robuste aux fichiers très actifs : deux fichiers
 * qui changent souvent mais rarement ensemble obtiennent un score faible.
 *
 * @param fileA         premier fichier (ordre canonique : fileA &lt; fileB)
 * @param fileB         second fichier
 * @param coChanges     nombre de commits où les deux ont changé ensemble
 * @param changesA      nombre total de commits touchant fileA
 * @param changesB      nombre total de commits touchant fileB
 * @param couplingScore indice de Jaccard (0..1)
 */
public record FileCoupling(
        String fileA,
        String fileB,
        int coChanges,
        int changesA,
        int changesB,
        double couplingScore) {
}