import { useCallback, useRef, useState } from 'react';
import { getAnalysisStatus, startAnalysis } from '../api/client';
import type {
  AnalysisProgress,
  AnalyzeRequest,
  RepositoryAnalysis,
} from '../types/analysis';

// État du cycle de vie de l'analyse, modélisé en union discriminée par `status`.
// La variante `loading` porte la progression (null tant que le 1er statut n'est
// pas revenu) ; impossible d'avoir `data` et `error` en même temps.
type AnalysisState =
  | { status: 'idle' }
  | { status: 'loading'; progress: AnalysisProgress | null }
  | { status: 'success'; data: RepositoryAnalysis }
  | { status: 'error'; error: string };

const POLL_INTERVAL_MS = 800;
const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Encapsule l'analyse asynchrone et son cycle de vie
 * (idle → loading(+progression) → success | error).
 *
 * Démarre un job côté API puis interroge sa progression en boucle, ce qui évite
 * de bloquer sur une longue requête et permet d'afficher une vraie progression.
 */
export function useAnalysis() {
  const [state, setState] = useState<AnalysisState>({ status: 'idle' });
  // Jeton de course : si une nouvelle analyse démarre, le polling de l'ancienne
  // s'arrête et n'écrase plus l'état (évite un résultat périmé).
  const runToken = useRef(0);

  const run = useCallback(async (req: AnalyzeRequest) => {
    const token = ++runToken.current;
    setState({ status: 'loading', progress: null });
    try {
      const { jobId } = await startAnalysis(req);

      while (runToken.current === token) {
        const job = await getAnalysisStatus(jobId);
        if (runToken.current !== token) return; // une autre analyse a pris la main

        if (job.status === 'DONE' && job.analysis) {
          setState({ status: 'success', data: job.analysis });
          return;
        }
        if (job.status === 'ERROR') {
          setState({
            status: 'error',
            error: job.message ?? "Échec de l'analyse.",
          });
          return;
        }
        setState({
          status: 'loading',
          progress: { step: job.step, current: job.current, total: job.total },
        });
        await sleep(POLL_INTERVAL_MS);
      }
    } catch (error) {
      if (runToken.current !== token) return;
      const message =
        error instanceof Error ? error.message : "Échec de l'analyse.";
      setState({ status: 'error', error: message });
    }
  }, []);

  return { state, run };
}
