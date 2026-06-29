// Helpers de formatage pour l'affichage. Fonctions pures, sans état, partagées
// par les composants. Les seuils de risque et la troncature de chemin reprennent
// la logique de la CLI (ReportFormatter) pour rester cohérent entre interfaces.

const dateTimeFmt = new Intl.DateTimeFormat('fr-FR', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

const dateFmt = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' });

const numberFmt = new Intl.NumberFormat('fr-FR');

/** Formate un Instant ISO-8601 (date + heure), ex. "28 juin 2026, 10:00". */
export function formatDateTime(iso: string): string {
  return dateTimeFmt.format(new Date(iso));
}

/** Formate une date seule (LocalDate "YYYY-MM-DD"), ex. "28 juin 2026". */
export function formatDate(iso: string): string {
  return dateFmt.format(new Date(iso));
}

/** Formate un entier avec séparateurs de milliers, ex. 12345 -> "12 345". */
export function formatNumber(n: number): string {
  return numberFmt.format(n);
}

/**
 * Tronque un chemin en gardant la FIN (la plus parlante), préfixée de "…".
 * Calque `ReportFormatter.truncatePath`.
 */
export function truncatePath(path: string, max = 48): string {
  if (path.length <= max) return path;
  const ellipsis = '…';
  return ellipsis + path.slice(path.length - (max - ellipsis.length));
}

/** Niveau de risque dérivé du ratio riskScore / maxRisk (seuils CLI). */
export type RiskLevel = 'high' | 'medium' | 'low';

/**
 * Classe un riskScore par rapport au max des hotspots.
 * ratio ≥ 0.66 → high (rouge), ≥ 0.33 → medium (jaune), sinon low (vert).
 */
export function riskLevel(score: number, maxRisk: number): RiskLevel {
  const ratio = maxRisk > 0 ? score / maxRisk : 0;
  if (ratio >= 0.66) return 'high';
  if (ratio >= 0.33) return 'medium';
  return 'low';
}

/** Classes Tailwind (pastille de couleur) associées à un niveau de risque. */
export const riskDotClasses: Record<RiskLevel, string> = {
  high: 'bg-red-500',
  medium: 'bg-amber-400',
  low: 'bg-emerald-500',
};

const percentFmt = new Intl.NumberFormat('fr-FR', {
  style: 'percent',
  maximumFractionDigits: 0,
});

/** Formate un ratio 0..1 en pourcentage, ex. 0.8 -> "80 %". */
export function formatPercent(ratio: number): string {
  return percentFmt.format(ratio);
}

/**
 * Niveau de "possession" d'un fichier selon son ownership (bus factor).
 * ≥ 0.8 → high (rouge, bus factor 1) ; ≥ 0.5 → medium (ambre) ; sinon low (vert).
 * Réutilise RiskLevel/riskDotClasses : un ownership élevé = un risque élevé.
 */
export function ownershipLevel(ownership: number): RiskLevel {
  if (ownership >= 0.8) return 'high';
  if (ownership >= 0.5) return 'medium';
  return 'low';
}
