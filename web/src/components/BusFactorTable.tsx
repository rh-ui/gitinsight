import { useMemo, useState } from 'react';
import type { FileOwnership } from '../types/analysis';
import {
  formatNumber,
  formatPercent,
  ownershipLevel,
  riskDotClasses,
  truncatePath,
} from '../lib/format';
import { usePagination } from '../hooks/usePagination';
import { Pagination } from './Pagination';

interface BusFactorTableProps {
  busFactor: FileOwnership[];
}

type SortKey = 'path' | 'topAuthor' | 'totalLines' | 'ownership';
type SortDir = 'asc' | 'desc';

const COLUMNS: { key: SortKey; label: string; numeric: boolean }[] = [
  { key: 'path', label: 'Fichier', numeric: false },
  { key: 'topAuthor', label: 'Auteur dominant', numeric: false },
  { key: 'totalLines', label: 'Lignes', numeric: true },
  { key: 'ownership', label: 'Ownership', numeric: true },
];

/**
 * Tableau triable et paginé du bus factor. Un ownership élevé (pastille rouge)
 * = fichier "possédé" par une seule personne → zone fragile (bus factor 1).
 */
export function BusFactorTable({ busFactor }: BusFactorTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>('ownership');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  const sorted = useMemo(() => {
    const copy = [...busFactor];
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
  }, [busFactor, sortKey, sortDir]);

  const pager = usePagination(sorted, 10);

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir(key === 'path' || key === 'topAuthor' ? 'asc' : 'desc');
    }
  }

  return (
    <section className="rounded-lg border border-border bg-surface p-4">
      <h2 className="mb-4 text-sm font-semibold text-foreground">Bus factor</h2>
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
            {pager.pageItems.map((fo, i) => (
              <tr
                key={fo.path}
                className={`text-foreground ${i % 2 === 1 ? 'bg-surface-2' : ''}`}
              >
                <td className="py-2">
                  <span className="flex items-center gap-2">
                    <span
                      className={`inline-block h-2 w-2 shrink-0 rounded-full ${riskDotClasses[ownershipLevel(fo.ownership)]}`}
                    />
                    <span className="font-mono text-xs" title={fo.path}>
                      {truncatePath(fo.path)}
                    </span>
                  </span>
                </td>
                <td className="py-2" title={fo.topAuthorEmail}>
                  {fo.topAuthor}
                </td>
                <td className="py-2 text-right font-mono">
                  {formatNumber(fo.topAuthorLines)} / {formatNumber(fo.totalLines)}
                </td>
                <td className="py-2 text-right font-mono tabular-nums">
                  {formatPercent(fo.ownership)}
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