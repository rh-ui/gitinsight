import axios from 'axios';
import type {
  AnalyzeRequest,
  ApiError,
  JobStartResponse,
  JobStatus,
  RepositoryAnalysis,
} from '../types/analysis';


const BASE_URL = import.meta.env.VITE_API_BASE_URL;

// Instance axios préconfigurée : toute requête part vers BASE_URL avec le bon
// en-tête JSON. Centraliser ici évite de répéter la config à chaque appel.
const http = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Lance l'analyse d'un dépôt via POST /api/analyze.
 *
 * Contrairement à `fetch`, axios rejette la promesse sur un statut 4xx/5xx :
 * on attrape donc l'erreur et on extrait le `message` du corps `ErrorResponse`
 * de l'API (avec un repli générique si le corps n'est pas exploitable).
 *
 * @throws Error message lisible destiné à l'affichage par l'UI.
 */
export async function analyze(
  req: AnalyzeRequest,
): Promise<RepositoryAnalysis> {
  try {
    const res = await http.post<RepositoryAnalysis>('/analyze', req);
    return res.data;
  } catch (error) {
    if (axios.isAxiosError<ApiError>(error)) {
      // Erreur HTTP avec corps applicatif (ErrorResponse) ou erreur réseau.
      const message =
        error.response?.data?.message ??
        error.message ??
        "Échec de l'analyse.";
      throw new Error(message);
    }
    throw error; // erreur non-axios inattendue : on la laisse remonter telle quelle.
  }
}

/**
 * Démarre une analyse asynchrone via POST /api/analyze/async.
 * Renvoie l'identifiant du job à interroger ensuite avec getAnalysisStatus.
 */
export async function startAnalysis(
  req: AnalyzeRequest,
): Promise<JobStartResponse> {
  try {
    const res = await http.post<JobStartResponse>('/analyze/async', req);
    return res.data;
  } catch (error) {
    if (axios.isAxiosError<ApiError>(error)) {
      const message =
        error.response?.data?.message ??
        error.message ??
        "Échec du démarrage de l'analyse.";
      throw new Error(message);
    }
    throw error;
  }
}

/**
 * Récupère l'avancement (et le résultat final) d'un job via
 * GET /api/analyze/status/{jobId}.
 */
export async function getAnalysisStatus(jobId: string): Promise<JobStatus> {
  const res = await http.get<JobStatus>(`/analyze/status/${jobId}`);
  return res.data;
}
