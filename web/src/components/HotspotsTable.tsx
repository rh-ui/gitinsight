import { useMemo, useState } from 'react';
import type { Hotspot } from '../types/analysis';
import { formatNumber, riskDotClasses, riskLevel, truncatePath } from '../lib/format';

interface HotspotsTableProps {
  hotspots: Hotspot[];
}

// Colonnes triables = clés numériques/texte de Hotspot.
type SortKey = keyof Hotspot;
type SortDir = 'asc' | 'desc';

const COLUMNS: { key: SortKey; label: string; numeric: boolean }[] = [
  { key: 'path', label: 'Fichier', numeric: false },
  { key: 'changeCount', label: 'Changements', numeric: true },
  { key: 'distinctAuthors', label: 'Auteurs', numeric: true },
  { key: 'riskScore', label: 'Risque', numeric: true },
];

/**
 * Tableau triable des fichiers à risque. Le tri est local (useState : clé +
 * sens). Une pastille de couleur traduit le riskScore relatif au max.
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
      // Comparaison adaptée au type (texte vs nombre).
      const cmp =
        typeof av === 'string' && typeof bv === 'string'
          ? av.localeCompare(bv)
          : Number(av) - Number(bv);
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return copy;
  }, [hotspots, sortKey, sortDir]);

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      // Même colonne : on inverse le sens.
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      // Nouvelle colonne : tri descendant pour les nombres, ascendant sinon.
      setSortKey(key);
      setSortDir(key === 'path' ? 'asc' : 'desc');
    }
  }

  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <h2 className="mb-4 text-sm font-semibold text-slate-200">
        Fichiers à risque
      </h2>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-xs uppercase tracking-wide text-slate-400">
              {COLUMNS.map((col) => (
                <th
                  key={col.key}
                  onClick={() => toggleSort(col.key)}
                  className={`cursor-pointer select-none py-2 font-medium hover:text-slate-200 ${
                    col.numeric ? 'text-right' : 'text-left'
                  }`}
                >
                  {col.label}
                  {sortKey === col.key && (
                    <span className="ml-1 text-indigo-400">
                      {sortDir === 'asc' ? '▲' : '▼'}
                    </span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.map((h) => (
              <tr
                key={h.path}
                className="border-t border-slate-800 text-slate-200"
              >
                <td className="py-2">
                  <span className="flex items-center gap-2">
                    <span
                      className={`inline-block h-2 w-2 rounded-full ${riskDotClasses[riskLevel(h.riskScore, maxRisk)]}`}
                    />
                    <span className="font-mono text-xs" title={h.path}>
                      {truncatePath(h.path)}
                    </span>
                  </span>
                </td>
                <td className="py-2 text-right">{formatNumber(h.changeCount)}</td>
                <td className="py-2 text-right">
                  {formatNumber(h.distinctAuthors)}
                </td>
                <td className="py-2 text-right tabular-nums">
                  {h.riskScore.toFixed(1)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
