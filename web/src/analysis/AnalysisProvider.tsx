import { createContext, useContext, type ReactNode } from 'react';
import { useAnalysis } from '../hooks/useAnalysis';

type AnalysisContextValue = ReturnType<typeof useAnalysis>;

const AnalysisContext = createContext<AnalysisContextValue | undefined>(
  undefined,
);

/**
 * Élève l'état d'analyse au-dessus des routes : l'analyse tourne une seule
 * fois et toutes les pages lisent le même résultat. S'appuie sur le hook
 * `useAnalysis` (aucune logique dupliquée ici).
 */
export function AnalysisProvider({ children }: { children: ReactNode }) {
  const analysis = useAnalysis();
  return (
    <AnalysisContext.Provider value={analysis}>
      {children}
    </AnalysisContext.Provider>
  );
}

/** Accès à l'état d'analyse partagé. Échoue hors de l'AnalysisProvider. */
export function useAnalysisContext(): AnalysisContextValue {
  const ctx = useContext(AnalysisContext);
  if (!ctx) {
    throw new Error(
      'useAnalysisContext doit être utilisé dans un <AnalysisProvider>.',
    );
  }
  return ctx;
}
