import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { WeeklyVelocity } from '../types/analysis';
import { formatDate, formatNumber } from '../lib/format';

interface VelocityChartProps {
  velocity: WeeklyVelocity[];
}

/**
 * Graphe de vélocité hebdomadaire : barres des lignes ajoutées/supprimées
 * (axe gauche) + ligne des commits (axe droit). Deux axes Y car commits et
 * lignes n'ont pas le même ordre de grandeur.
 */
export function VelocityChart({ velocity }: VelocityChartProps) {
  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
      <h2 className="mb-4 text-sm font-semibold text-slate-200">
        Vélocité hebdomadaire
      </h2>
      {/* ResponsiveContainer : le graphe s'adapte à la largeur du parent. */}
      <ResponsiveContainer width="100%" height={300}>
        <ComposedChart
          data={velocity}
          margin={{ top: 8, right: 8, bottom: 8, left: 8 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
          <XAxis
            dataKey="weekStart"
            tickFormatter={formatDate}
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            stroke="#334155"
          />
          {/* Axe gauche : lignes de code. */}
          <YAxis
            yAxisId="lines"
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            stroke="#334155"
          />
          {/* Axe droit : commits. */}
          <YAxis
            yAxisId="commits"
            orientation="right"
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            stroke="#334155"
            allowDecimals={false}
          />
          <Tooltip
            labelFormatter={(label) => formatDate(String(label))}
            formatter={(value: number) => formatNumber(value)}
            contentStyle={{
              background: '#0f172a',
              border: '1px solid #334155',
              borderRadius: 8,
              color: '#e2e8f0',
            }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          <Bar
            yAxisId="lines"
            dataKey="linesAdded"
            name="Lignes ajoutées"
            fill="#10b981"
            radius={[2, 2, 0, 0]}
          />
          <Bar
            yAxisId="lines"
            dataKey="linesDeleted"
            name="Lignes supprimées"
            fill="#ef4444"
            radius={[2, 2, 0, 0]}
          />
          <Line
            yAxisId="commits"
            type="monotone"
            dataKey="commits"
            name="Commits"
            stroke="#818cf8"
            strokeWidth={2}
            dot={false}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </section>
  );
}
