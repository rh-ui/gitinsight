import { AnalysisGate } from '../components/AnalysisGate';
import { BusFactorTable } from '../components/BusFactorTable';

/** Bus factor : qui "possède" les lignes des fichiers (ownership au HEAD). */
export function BusFactorPage() {
  return (
    <div className="flex flex-col gap-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold text-foreground">Bus factor</h2>
        <div className="flex items-center gap-3 text-xs text-muted">
          <span className="flex items-center gap-1">
            <span className="inline-block h-2 w-2 rounded-full bg-red-500" /> ≥ 80 %
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-2 w-2 rounded-full bg-amber-400" /> ≥ 50 %
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-2 w-2 rounded-full bg-emerald-500" /> &lt; 50 %
          </span>
        </div>
      </div>
      <AnalysisGate>{(data) => <BusFactorTable busFactor={data.busFactor} />}</AnalysisGate>
    </div>
  );
}