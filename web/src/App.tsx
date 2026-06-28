import { useAnalysis } from './hooks/useAnalysis';
import { AnalyzeForm } from './components/AnalyzeForm';
import { SummaryCards } from './components/SummaryCards';
import { VelocityChart } from './components/VelocityChart';
import { AuthorsChart } from './components/AuthorsChart';
import { HotspotsTable } from './components/HotspotsTable';

function App() {
  const { state, run } = useAnalysis();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto max-w-5xl px-4 py-8">
        <header className="mb-8">
          <h1 className="text-2xl font-bold tracking-tight">
            Git<span className="text-indigo-400">Insight</span>
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Analyse de l'historique Git : vélocité, auteurs et fichiers à risque.
          </p>
        </header>

        <div className="mb-8 rounded-xl border border-slate-800 bg-slate-900/40 p-4">
          <AnalyzeForm onAnalyze={run} loading={state.status === 'loading'} />
        </div>

        {state.status === 'idle' && (
          <p className="rounded-xl border border-dashed border-slate-800 p-8 text-center text-slate-500">
            Saisis le chemin d'un dépôt local puis lance l'analyse.
          </p>
        )}

        {state.status === 'loading' && (
          <div className="flex items-center justify-center gap-3 p-12 text-slate-400">
            <span className="h-5 w-5 animate-spin rounded-full border-2 border-slate-600 border-t-indigo-400" />
            Analyse en cours…
          </div>
        )}

        {state.status === 'error' && (
          <div className="rounded-xl border border-red-900 bg-red-950/40 p-4 text-red-300">
            <strong className="font-semibold">Erreur :</strong> {state.error}
          </div>
        )}

        {state.status === 'success' && (
          <div className="flex flex-col gap-6">
            <SummaryCards data={state.data} />
            <VelocityChart velocity={state.data.velocity} />
            <AuthorsChart authors={state.data.authors} />
            <HotspotsTable hotspots={state.data.hotspots} />
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
