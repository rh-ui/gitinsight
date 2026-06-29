import { useState, type FormEvent } from 'react';
import { Play } from 'lucide-react';
import type { AnalyzeRequest } from '../types/analysis';

interface AnalyzeFormProps {
  /** Déclenche l'analyse avec les valeurs saisies. */
  onAnalyze: (req: AnalyzeRequest) => void;
  /** Vrai pendant l'appel API : désactive le formulaire. */
  loading: boolean;
}

const inputClasses =
  'rounded border border-border bg-background px-3 py-1.5 text-sm text-foreground placeholder-muted outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/30 disabled:opacity-50';

/**
 * Formulaire de lancement d'analyse : source du dépôt + nombre de hotspots.
 * La source peut être un chemin local OU une URL distante HTTP(S) ; le serveur
 * clone une URL avant de l'analyser. Composant contrôlé et « bête » : il délègue
 * l'analyse à `onAnalyze`.
 */
export function AnalyzeForm({ onAnalyze, loading }: AnalyzeFormProps) {
  const [path, setPath] = useState('.');
  const [topHotspots, setTopHotspots] = useState(10);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onAnalyze({ path: path.trim(), topHotspots });
  }

  return (
    <form onSubmit={handleSubmit} className="flex items-center gap-2">
      <input
        type="text"
        value={path}
        onChange={(e) => setPath(e.target.value)}
        placeholder="Chemin local ou URL (ex. . ou https://github.com/org/repo.git)"
        disabled={loading}
        className={`${inputClasses} min-w-0 flex-1 font-mono`}
        aria-label="Chemin local ou URL du dépôt"
      />
      <input
        type="number"
        min={1}
        value={topHotspots}
        onChange={(e) => setTopHotspots(Number(e.target.value))}
        disabled={loading}
        className={`${inputClasses} w-20`}
        aria-label="Nombre de hotspots"
      />
      <button
        type="submit"
        disabled={loading}
        className="flex shrink-0 items-center gap-2 rounded bg-primary px-4 py-1.5 text-sm font-bold text-on-primary transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Play size={16} />
        {loading ? 'Analyse…' : 'Analyser'}
      </button>
    </form>
  );
}
