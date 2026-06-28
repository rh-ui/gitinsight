import { AnalysisGate } from '../components/AnalysisGate';
import { HotspotsTable } from '../components/HotspotsTable';

/** Fichiers à risque : tableau triable (pagination ajoutée en R5). */
export function HotspotsPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Fichiers à risque</h1>
      <AnalysisGate>{(data) => <HotspotsTable hotspots={data.hotspots} />}</AnalysisGate>
    </div>
  );
}
