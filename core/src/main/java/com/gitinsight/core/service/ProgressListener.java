package com.gitinsight.core.service;

/**
 * Rapporte l'avancement d'une analyse, étape par étape.
 *
 * <p>
 * Le moteur reste découplé de l'UI : il ne connaît ni HTTP, ni job, ni barre de
 * progression. Il se contente de notifier « j'attaque l'étape X (i/N) ».
 * L'appelant (API asynchrone, CLI…) décide quoi en faire. {@link #NOOP} permet
 * d'ignorer la progression (chemin synchrone, tests).
 */
@FunctionalInterface
public interface ProgressListener {

    /**
     * @param step    libellé lisible de l'étape qui démarre (ex. « Bus factor »).
     * @param current index de l'étape qui démarre (0 = pré-traitement/clonage).
     * @param total   nombre total d'étapes.
     */
    void onProgress(String step, int current, int total);

    /** Écoute neutre : n'enregistre rien. */
    ProgressListener NOOP = (step, current, total) -> {
    };
}
