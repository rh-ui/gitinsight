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
import { chartChrome, SERIES } from '../lib/chartColors';
import { useTheme } from '../theme/ThemeProvider';

interface VelocityChartProps {
  velocity: WeeklyVelocity[];
}

/**
 * Vélocité hebdomadaire : barres lignes +/- (axe gauche) + ligne des commits
 * (axe droit). La « chrome » (grille/axes/tooltip) suit le thème courant.
 */
export function VelocityChart({ velocity }: VelocityChartProps) {
  const { theme } = useTheme();
  const c = chartChrome(theme);

  return (
    <section className="rounded-lg border border-border bg-surface p-4">
      <h2 className="mb-4 text-sm font-semibold text-foreground">
        Vélocité hebdomadaire
      </h2>
      <ResponsiveContainer width="100%" height={300}>
        <ComposedChart
          data={velocity}
          margin={{ top: 8, right: 8, bottom: 8, left: 8 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke={c.grid} />
          <XAxis
            dataKey="weekStart"
            tickFormatter={formatDate}
            tick={{ fill: c.tick, fontSize: 12 }}
            stroke={c.axis}
          />
          <YAxis yAxisId="lines" tick={{ fill: c.tick, fontSize: 12 }} stroke={c.axis} />
          <YAxis
            yAxisId="commits"
            orientation="right"
            tick={{ fill: c.tick, fontSize: 12 }}
            stroke={c.axis}
            allowDecimals={false}
          />
          <Tooltip
            labelFormatter={(label) => formatDate(String(label))}
            formatter={(value: number) => formatNumber(value)}
            contentStyle={{
              background: c.tooltipBg,
              border: `1px solid ${c.tooltipBorder}`,
              borderTop: `2px solid ${c.tooltipBorder}`,
              borderRadius: 6,
              color: c.tooltipText,
            }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          <Bar
            yAxisId="lines"
            dataKey="linesAdded"
            name="Lignes ajoutées"
            fill={SERIES.linesAdded}
            radius={[2, 2, 0, 0]}
          />
          <Bar
            yAxisId="lines"
            dataKey="linesDeleted"
            name="Lignes supprimées"
            fill={SERIES.linesDeleted}
            radius={[2, 2, 0, 0]}
          />
          <Line
            yAxisId="commits"
            type="monotone"
            dataKey="commits"
            name="Commits"
            stroke={SERIES.commits}
            strokeWidth={2}
            dot={false}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </section>
  );
}
