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
  busFactor: FileOwnership[];
  coupling: FileCoupling[];
}

/** Corps de la requête POST /api/analyze — reflète AnalyzeRequest. */
export interface AnalyzeRequest {
  path: string;
  topHotspots: number;
  topCoupling?: number; // optionnel : défaut serveur (30) si absent
}

/** Corps d'erreur renvoyé en 400/500 — reflète ErrorResponse. */
export interface ApiError {
  status: number;
  error: string;
  message: string;
}

/** Propriété des lignes d'un fichier au HEAD — reflète FileOwnership. */
export interface FileOwnership {
  path: string;
  topAuthor: string;
  topAuthorEmail: string;
  topAuthorLines: number;
  totalLines: number;
  ownership: number; // 0..1
}

/** Couplage temporel entre deux fichiers — reflète FileCoupling. */
export interface FileCoupling {
  fileA: string;
  fileB: string;
  coChanges: number;
  changesA: number;
  changesB: number;
  couplingScore: number; // 0..1 (Jaccard)
}

/** Réponse de POST /api/analyze/async — reflète JobStartResponse. */
export interface JobStartResponse {
  jobId: string;
}

/** Avancement d'une analyse en cours (étape courante sur total). */
export interface AnalysisProgress {
  step: string;
  current: number;
  total: number;
}

/** État d'un job d'analyse — reflète JobStatusResponse. */
export interface JobStatus {
  status: 'RUNNING' | 'DONE' | 'ERROR';
  step: string;
  current: number;
  total: number;
  analysis: RepositoryAnalysis | null; // présent quand status === 'DONE'
  message: string | null; // présent quand status === 'ERROR'
}
