// Types miroir des records Java du module `core` (com.gitinsight.core.model).
// Spring sérialise les `Instant` en chaînes ISO-8601 et les `LocalDate` en
// "YYYY-MM-DD" ; on les représente donc en `string` côté front.

/** Métadonnées globales de l'analyse — reflète AnalysisMeta. */
export interface AnalysisMeta {
  totalCommits: number;
  firstCommit: string; // Instant -> ISO-8601
  lastCommit: string; // Instant -> ISO-8601
  generatedAt: string; // Instant -> ISO-8601
}

/** Activité d'une semaine — reflète WeeklyVelocity. */
export interface WeeklyVelocity {
  weekStart: string; // LocalDate -> "YYYY-MM-DD"
  commits: number;
  linesAdded: number;
  linesDeleted: number;
  activeAuthors: number;
}

/** Statistiques par auteur — reflète AuthorStats. */
export interface AuthorStats {
  name: string;
  email: string;
  commits: number;
  filesTouched: number;
  linesAdded: number;
  linesDeleted: number;
}

/** Fichier à risque — reflète Hotspot. */
export interface Hotspot {
  path: string;
  changeCount: number;
  distinctAuthors: number;
  riskScore: number;
}

/** Réponse complète de POST /api/analyze — reflète RepositoryAnalysis. */
export interface RepositoryAnalysis {
  meta: AnalysisMeta;
  velocity: WeeklyVelocity[];
  authors: AuthorStats[];
  hotspots: Hotspot[];
}

/** Corps de la requête POST /api/analyze — reflète AnalyzeRequest. */
export interface AnalyzeRequest {
  path: string;
  topHotspots: number;
}

/** Corps d'erreur renvoyé en 400/500 — reflète ErrorResponse. */
export interface ApiError {
  status: number;
  error: string;
  message: string;
}
