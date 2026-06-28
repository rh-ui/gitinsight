import { AnalysisGate } from '../components/AnalysisGate';
import { AuthorsChart } from '../components/AuthorsChart';

/** Répartition par auteur : bar chart + mini-table. */
export function AuthorsPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Auteurs</h1>
      <AnalysisGate>{(data) => <AuthorsChart authors={data.authors} />}</AnalysisGate>
    </div>
  );
}
