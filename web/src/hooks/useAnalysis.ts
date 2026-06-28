import { useCallback, useState } from 'react';
import { analyze } from '../api/client';
import type { AnalyzeRequest, RepositoryAnalysis } from '../types/analysis';

// État du cycle de vie de l'analyse, modélisé en union discriminée par `status`.
// Chaque variante ne porte que les données pertinentes : impossible d'avoir
// `data` et `error` en même temps.
type AnalysisState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: RepositoryAnalysis }
  | { status: 'error'; error: string };

/**
 * Encapsule l'appel à l'API d'analyse et son cycle de vie
 * (idle → loading → success | error).
 *
 * Garde `App` simple : pas de lib de data-fetching (un seul endpoint suffit).
 * Expose l'état courant et une fonction `run` pour déclencher l'analyse.
 */
export function useAnalysis() {
  const [state, setState] = useState<AnalysisState>({ status: 'idle' });

  // `useCallback` : `run` garde la même référence entre les rendus tant que
  // ses dépendances ne changent pas (ici aucune) — utile si on la passe à un
  // composant enfant ou à un effet.
  const run = useCallback(async (req: AnalyzeRequest) => {
    setState({ status: 'loading' });
    try {
      const data = await analyze(req);
      setState({ status: 'success', data });
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Échec de l'analyse.";
      setState({ status: 'error', error: message });
    }
  }, []);

  return { state, run };
}
