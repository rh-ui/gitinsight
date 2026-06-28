import { useMemo } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { AuthorStats } from '../types/analysis';
import { formatNumber } from '../lib/format';

interface AuthorsChartProps {
  authors: AuthorStats[];
}

/**
 * Répartition des commits par auteur : bar chart horizontal trié (le plus
 * actif en haut), suivi d'une mini-table (fichiers touchés, lignes +/-).
 */
export function AuthorsChart({ authors }: AuthorsChartProps) {
  // Tri décroissant par commits. On copie ([...]) pour ne pas muter la prop.
  const sorted = useMemo(
    () => [...authors].sort((a, b) => b.commits - a.commits),
    [authors],
  );

  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <h2 className="mb-4 text-sm font-semibold text-slate-200">
        Répartition par auteur
      </h2>

      <ResponsiveContainer width="100%" height={Math.max(180, sorted.length * 40)}>
        <BarChart
          data={sorted}
          layout="vertical"
          margin={{ top: 4, right: 16, bottom: 4, left: 8 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" horizontal={false} />
          <XAxis
            type="number"
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            stroke="#334155"
            allowDecimals={false}
          />
          <YAxis
            type="category"
            dataKey="name"
            width={120}
            tick={{ fill: '#cbd5e1', fontSize: 12 }}
            stroke="#334155"
          />
          <Tooltip
            formatter={(value: number) => formatNumber(value)}
            cursor={{ fill: '#1e293b55' }}
            contentStyle={{
              background: '#0f172a',
              border: '1px solid #334155',
              borderRadius: 8,
              color: '#e2e8f0',
            }}
          />
          <Bar dataKey="commits" name="Commits" fill="#818cf8" radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>

      {/* Mini-table : détails par auteur. */}
      <table className="mt-4 w-full text-sm">
        <thead>
          <tr className="text-left text-xs uppercase tracking-wide text-slate-400">
            <th className="py-2 font-medium">Auteur</th>
            <th className="py-2 text-right font-medium">Commits</th>
            <th className="py-2 text-right font-medium">Fichiers</th>
            <th className="py-2 text-right font-medium">Lignes +/−</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((a) => (
            <tr key={a.email} className="border-t border-slate-800 text-slate-200">
              <td className="py-2">
                <div className="font-medium">{a.name}</div>
                <div className="text-xs text-slate-500">{a.email}</div>
              </td>
              <td className="py-2 text-right">{formatNumber(a.commits)}</td>
              <td className="py-2 text-right">{formatNumber(a.filesTouched)}</td>
              <td className="py-2 text-right">
                <span className="text-emerald-400">+{formatNumber(a.linesAdded)}</span>{' '}
                <span className="text-red-400">−{formatNumber(a.linesDeleted)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
