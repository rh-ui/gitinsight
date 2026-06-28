import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { ThemeToggle } from '../ThemeToggle';
import { AnalyzeForm } from '../AnalyzeForm';
import { useAnalysisContext } from '../../analysis/AnalysisProvider';

/**
 * Coquille persistante de l'application : sidebar de navigation, topbar
 * (formulaire d'analyse + bascule de thème) et zone de contenu où React
 * Router injecte la page courante via <Outlet/>.
 */
export function AppLayout() {
  const { state, run } = useAnalysisContext();

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center gap-4 border-b border-border bg-surface px-6 py-3">
          <div className="min-w-0 flex-1">
            <AnalyzeForm onAnalyze={run} loading={state.status === 'loading'} />
          </div>
          <ThemeToggle />
        </header>

        <main className="mx-auto w-full max-w-[1440px] flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
