import { AnalysisGate } from '../components/AnalysisGate';
import { BusFactorTable } from '../components/BusFactorTable';

/** Bus factor : qui "possède" les lignes des fichiers (ownership au HEAD). */
export function BusFactorPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Bus factor</h1>
      <AnalysisGate>{(data) => <BusFactorTable busFactor={data.busFactor} />}</AnalysisGate>
    </div>
  );
}