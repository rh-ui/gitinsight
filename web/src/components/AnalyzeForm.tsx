import { useState, type FormEvent } from 'react';
import type { AnalyzeRequest } from '../types/analysis';

interface AnalyzeFormProps {
  /** Déclenche l'analyse avec les valeurs saisies. */
  onAnalyze: (req: AnalyzeRequest) => void;
  /** Vrai pendant l'appel API : désactive le formulaire. */
  loading: boolean;
}

/**
 * Formulaire de lancement d'analyse : chemin du dépôt + nombre de hotspots.
 * Composant contrôlé (les valeurs vivent dans le state) et « bête » : il
 * délègue l'analyse à `onAnalyze`, sans connaître l'API.
 */
export function AnalyzeForm({ onAnalyze, loading }: AnalyzeFormProps) {
  const [path, setPath] = useState('.');
  const [topHotspots, setTopHotspots] = useState(10);

  function handleSubmit(event: FormEvent) {
    event.preventDefault(); // empêche le rechargement de page natif du <form>
    onAnalyze({ path: path.trim(), topHotspots });
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-4 sm:flex-row sm:items-end"
    >
      <label className="flex flex-1 flex-col gap-1">
        <span className="text-sm font-medium text-slate-300">
          Chemin du dépôt
        </span>
        <input
          type="text"
          value={path}
          onChange={(e) => setPath(e.target.value)}
          placeholder="."
          disabled={loading}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 placeholder-slate-500 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/40 disabled:opacity-50"
        />
      </label>

      <label className="flex flex-col gap-1 sm:w-40">
        <span className="text-sm font-medium text-slate-300">Hotspots</span>
        <input
          type="number"
          min={1}
          value={topHotspots}
          onChange={(e) => setTopHotspots(Number(e.target.value))}
          disabled={loading}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/40 disabled:opacity-50"
        />
      </label>

      <button
        type="submit"
        disabled={loading}
        className="rounded-lg bg-indigo-600 px-5 py-2 font-medium text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {loading ? 'Analyse…' : 'Analyser'}
      </button>
    </form>
  );
}
