// Couleurs des graphes Recharts. Les SÉRIES sont constantes (signaux
// sémantiques) ; la « chrome » (grille, axes, tooltip) s'adapte au thème
// car Recharts exige des valeurs hex, pas des classes Tailwind.

export const SERIES = {
  commits: '#f47d20', // Forge Orange
  linesAdded: '#22c55e', // safe-green
  linesDeleted: '#ef4444', // hotspot-red
  authors: '#f47d20', // Forge Orange
} as const;

export interface ChartChrome {
  grid: string;
  axis: string;
  tick: string;
  tooltipBg: string;
  tooltipBorder: string;
  tooltipText: string;
}

/** Couleurs de grille/axes/tooltip selon le thème actif. */
export function chartChrome(theme: 'light' | 'dark'): ChartChrome {
  if (theme === 'dark') {
    return {
      grid: '#334155',
      axis: '#334155',
      tick: '#94a3b8',
      tooltipBg: '#1e293b',
      tooltipBorder: '#f47d20', // top-border orange (DESIGN.md)
      tooltipText: '#e2e8f0',
    };
  }
  return {
    grid: '#e2e8f0',
    axis: '#cbd5e1',
    tick: '#64748b',
    tooltipBg: '#ffffff',
    tooltipBorder: '#f47d20',
    tooltipText: '#0f172a',
  };
}
