import { AnalysisGate } from '../components/AnalysisGate';
import { SummaryCards } from '../components/SummaryCards';
import { VelocityChart } from '../components/VelocityChart';

/** Vue d'ensemble : cartes de synthèse + graphe de vélocité. */
export function OverviewPage() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Vue d'ensemble</h1>
      <AnalysisGate>
        {(data) => (
          <>
            <SummaryCards data={data} />
            <VelocityChart velocity={data.velocity} />
          </>
        )}
      </AnalysisGate>
    </div>
  );
}
