import { useMemo, type ReactNode } from 'react';
import type { RepositoryAnalysis } from '../types/analysis';
import { formatDate, formatDateTime, formatNumber } from '../lib/format';

interface SummaryCardsProps {
  data: RepositoryAnalysis;
}

/** Une carte : libellé, valeur principale, et détail optionnel. */
function Card({
  label,
  value,
  hint,
}: {
  label: string;
  value: ReactNode;
  hint?: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-surface p-4 transition hover:border-primary">
      <div className="font-mono text-xs font-bold uppercase tracking-wide text-muted">
        {label}
      </div>
      <div className="mt-1 text-2xl font-semibold text-foreground">{value}</div>
      {hint && <div className="mt-1 text-xs text-muted">{hint}</div>}
    </div>
  );
}

/**
 * Vue d'ensemble de l'analyse. Affiche des totaux directs (meta) et des
 * dérivés calculés à partir de `velocity[]` (lignes +/-, semaines actives).
 */
export function SummaryCards({ data }: SummaryCardsProps) {
  const { meta, velocity, authors } = data;

  const { linesAdded, linesDeleted } = useMemo(
    () =>
      velocity.reduce(
        (acc, w) => ({
          linesAdded: acc.linesAdded + w.linesAdded,
          linesDeleted: acc.linesDeleted + w.linesDeleted,
        }),
        { linesAdded: 0, linesDeleted: 0 },
      ),
    [velocity],
  );

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
      <Card label="Commits" value={formatNumber(meta.totalCommits)} />
      <Card label="Auteurs" value={formatNumber(authors.length)} />
      <Card label="Semaines actives" value={formatNumber(velocity.length)} />
      <Card
        label="Période"
        value={formatDate(meta.firstCommit)}
        hint={`→ ${formatDate(meta.lastCommit)}`}
      />
      <Card
        label="Lignes"
        value={
          <span className="font-mono">
            <span className="text-success">+{formatNumber(linesAdded)}</span>{' '}
            <span className="text-danger">−{formatNumber(linesDeleted)}</span>
          </span>
        }
      />
      <Card
        label="Généré le"
        value={
          <span className="text-base">{formatDateTime(meta.generatedAt)}</span>
        }
      />
    </div>
  );
}
