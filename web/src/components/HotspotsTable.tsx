import { useMemo, useState } from 'react';
import type { Hotspot } from '../types/analysis';
import { formatNumber, riskDotClasses, riskLevel, truncatePath } from '../lib/format';
import { usePagination } from '../hooks/usePagination';
import { Pagination } from './Pagination';

interface HotspotsTableProps {
  hotspots: Hotspot[];
}

type SortKey = keyof Hotspot;
type SortDir = 'asc' | 'desc';

const COLUMNS: { key: SortKey; label: string; numeric: boolean }[] = [
  { key: 'path', label: 'Fichier', numeric: false },
  { key: 'changeCount', label: 'Changements', numeric: true },
  { key: 'distinctAuthors', label: 'Auteurs', numeric: true },
  { key: 'riskScore', label: 'Risque', numeric: true },
];

/**
 * Tableau triable et paginé des fichiers à risque. Tri local (clé + sens),
 * pagination via usePagination, pastille de couleur selon le riskScore relatif.
 */
export function HotspotsTable({ hotspots }: HotspotsTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>('riskScore');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  const maxRisk = useMemo(
    () => Math.max(...hotspots.map((h) => h.riskScore), 0),
    [hotspots],
  );

  const sorted = useMemo(() => {
    const copy = [...hotspots];
    copy.sort((a, b) => {
      const av = a[sortKey];
      const bv = b[sortKey];
      const cmp =
        typeof av === 'string' && typeof bv === 'string'
          ? av.localeCompare(bv)
          : Number(av) - Number(bv);
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return copy;
  }, [hotspots, sortKey, sortDir]);

  const pager = usePagination(sorted, 10);

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir(key === 'path' ? 'asc' : 'desc');
    }
  }

  return (
    <section className="rounded-lg border border-border bg-surface p-4">
      <h2 className="mb-4 text-sm font-semibold text-foreground">
        Fichiers à risque
      </h2>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="font-mono text-xs uppercase tracking-wide text-muted">
              {COLUMNS.map((col) => {
                const active = sortKey === col.key;
                return (
                  <th
                    key={col.key}
                    onClick={() => toggleSort(col.key)}
                    className={`cursor-pointer select-none border-b-2 py-2 font-bold transition hover:text-foreground ${
                      col.numeric ? 'text-right' : 'text-left'
                    } ${active ? 'border-primary text-primary' : 'border-transparent'}`}
                  >
                    {col.label}
                    {active && <span className="ml-1">{sortDir === 'asc' ? '▲' : '▼'}</span>}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {pager.pageItems.map((h, i) => (
              <tr
                key={h.path}
                className={`text-foreground ${i % 2 === 1 ? 'bg-surface-2' : ''}`}
              >
                <td className="py-2">
                  <span className="flex items-center gap-2">
                    <span
                      className={`inline-block h-2 w-2 shrink-0 rounded-full ${riskDotClasses[riskLevel(h.riskScore, maxRisk)]}`}
                    />
                    <span className="font-mono text-xs" title={h.path}>
                      {truncatePath(h.path)}
                    </span>
                  </span>
                </td>
                <td className="py-2 text-right font-mono">{formatNumber(h.changeCount)}</td>
                <td className="py-2 text-right font-mono">
                  {formatNumber(h.distinctAuthors)}
                </td>
                <td className="py-2 text-right font-mono tabular-nums">
                  {h.riskScore.toFixed(1)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination
        page={pager.page}
        pageCount={pager.pageCount}
        rangeStart={pager.rangeStart}
        rangeEnd={pager.rangeEnd}
        total={pager.total}
        canPrev={pager.canPrev}
        canNext={pager.canNext}
        onPrev={pager.prev}
        onNext={pager.next}
      />
    </section>
  );
}
