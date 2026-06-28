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
import { chartChrome, SERIES } from '../lib/chartColors';
import { useTheme } from '../theme/ThemeProvider';

interface AuthorsChartProps {
  authors: AuthorStats[];
}

/**
 * Répartition des commits par auteur : bar chart horizontal trié, suivi d'une
 * mini-table (fichiers touchés, lignes +/-).
 */
export function AuthorsChart({ authors }: AuthorsChartProps) {
  const { theme } = useTheme();
  const c = chartChrome(theme);

  const sorted = useMemo(
    () => [...authors].sort((a, b) => b.commits - a.commits),
    [authors],
  );

  return (
    <section className="rounded-lg border border-border bg-surface p-4">
      <h2 className="mb-4 text-sm font-semibold text-foreground">
        Répartition par auteur
      </h2>

      <ResponsiveContainer width="100%" height={Math.max(180, sorted.length * 40)}>
        <BarChart
          data={sorted}
          layout="vertical"
          margin={{ top: 4, right: 16, bottom: 4, left: 8 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke={c.grid} horizontal={false} />
          <XAxis
            type="number"
            tick={{ fill: c.tick, fontSize: 12 }}
            stroke={c.axis}
            allowDecimals={false}
          />
          <YAxis
            type="category"
            dataKey="name"
            width={120}
            tick={{ fill: c.tick, fontSize: 12 }}
            stroke={c.axis}
          />
          <Tooltip
            formatter={(value: number) => formatNumber(value)}
            cursor={{ fill: c.grid, fillOpacity: 0.3 }}
            contentStyle={{
              background: c.tooltipBg,
              border: `1px solid ${c.tooltipBorder}`,
              borderTop: `2px solid ${c.tooltipBorder}`,
              borderRadius: 6,
              color: c.tooltipText,
            }}
          />
          <Bar dataKey="commits" name="Commits" fill={SERIES.authors} radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>

      <table className="mt-4 w-full text-sm">
        <thead>
          <tr className="text-left font-mono text-xs uppercase tracking-wide text-muted">
            <th className="py-2 font-bold">Auteur</th>
            <th className="py-2 text-right font-bold">Commits</th>
            <th className="py-2 text-right font-bold">Fichiers</th>
            <th className="py-2 text-right font-bold">Lignes +/−</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((a) => (
            <tr key={a.email} className="border-t border-border text-foreground">
              <td className="py-2">
                <div className="font-medium">{a.name}</div>
                <div className="font-mono text-xs text-muted">{a.email}</div>
              </td>
              <td className="py-2 text-right font-mono">{formatNumber(a.commits)}</td>
              <td className="py-2 text-right font-mono">{formatNumber(a.filesTouched)}</td>
              <td className="py-2 text-right font-mono">
                <span className="text-success">+{formatNumber(a.linesAdded)}</span>{' '}
                <span className="text-danger">−{formatNumber(a.linesDeleted)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
