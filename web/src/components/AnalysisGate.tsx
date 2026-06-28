import { Loader2, SearchCode, TriangleAlert } from 'lucide-react';
import type { ReactNode } from 'react';
import { useAnalysisContext } from '../analysis/AnalysisProvider';
import type { RepositoryAnalysis } from '../types/analysis';

interface AnalysisGateProps {
  /** Rendu appelé uniquement en cas de succès, avec les données. */
  children: (data: RepositoryAnalysis) => ReactNode;
}

/**
 * Gère les états idle/loading/error de l'analyse partagée et ne délègue à la
 * page (via render prop) qu'en cas de succès. Évite de dupliquer ce switch
 * dans chaque page.
 */
export function AnalysisGate({ children }: AnalysisGateProps) {
  const { state } = useAnalysisContext();

  if (state.status === 'idle') {
    return (
      <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-border p-12 text-center text-muted">
        <SearchCode size={28} />
        <p>Saisis le chemin d'un dépôt puis lance l'analyse depuis la barre du haut.</p>
      </div>
    );
  }

  if (state.status === 'loading') {
    return (
      <div className="flex items-center justify-center gap-3 p-12 text-muted">
        <Loader2 className="animate-spin text-primary" size={20} />
        Analyse en cours…
      </div>
    );
  }

  if (state.status === 'error') {
    return (
      <div className="flex items-start gap-3 rounded-lg border border-danger/40 bg-danger/10 p-4 text-danger">
        <TriangleAlert size={20} className="mt-0.5 shrink-0" />
        <div>
          <strong className="font-semibold">Erreur :</strong> {state.error}
        </div>
      </div>
    );
  }

  // status === 'success'
  return <>{children(state.data)}</>;
}
