package com.gitinsight.api.service;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.service.AnalysisService;
import com.gitinsight.core.service.ProgressListener;

/**
 * Exécute les analyses en arrière-plan et expose leur progression.
 *
 * <p>
 * Une analyse de dépôt (blame + couplage sur tout l'historique) peut durer
 * longtemps. Plutôt que de bloquer la requête HTTP, on lance le travail sur un
 * pool de threads et on renvoie immédiatement un identifiant de job. Le client
 * interroge ensuite la progression via {@link #get(String)}.
 *
 * <p>
 * Les jobs sont conservés en mémoire (suffisant pour un outil mono-instance). Une
 * éviction TTL serait l'étape suivante pour éviter une croissance non bornée.
 */
public class AnalysisJobService {

    public enum Status {
        RUNNING, DONE, ERROR
    }

    /** État mutable d'un job, mis à jour par le thread d'analyse (champs volatile). */
    public static final class Job {
        private volatile Status status = Status.RUNNING;
        private volatile String step = "En file d'attente";
        private volatile int current = 0;
        private volatile int total = 6;
        private volatile RepositoryAnalysis result;
        private volatile String error;

        Job() {
        }

        public Status status() {
            return status;
        }

        public String step() {
            return step;
        }

        public int current() {
            return current;
        }

        public int total() {
            return total;
        }

        public RepositoryAnalysis result() {
            return result;
        }

        public String error() {
            return error;
        }
    }

    private final AnalysisService analysisService;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public AnalysisJobService(AnalysisService analysisService) {
        this.analysisService = analysisService;
        // Threads démons : n'empêchent pas l'arrêt de l'application.
        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "analysis-job");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Démarre une analyse en arrière-plan et renvoie l'identifiant du job. */
    public String submit(Path source, int topHotspots, Integer topCoupling) {
        String id = UUID.randomUUID().toString();
        Job job = new Job();
        jobs.put(id, job);
        executor.submit(() -> run(job, source, topHotspots, topCoupling));
        return id;
    }

    /** Renvoie le job, ou {@code null} si l'identifiant est inconnu. */
    public Job get(String id) {
        return jobs.get(id);
    }

    private void run(Job job, Path source, int topHotspots, Integer topCoupling) {
        ProgressListener listener = (step, current, total) -> {
            job.step = step;
            job.current = current;
            job.total = total;
        };
        try {
            RepositoryAnalysis analysis = (topCoupling == null)
                    ? analysisService.analyze(source, topHotspots, listener)
                    : analysisService.analyze(source, topHotspots, topCoupling, listener);
            job.result = analysis;
            job.step = "Terminé";
            job.current = job.total;
            job.status = Status.DONE;
        } catch (Exception e) {
            job.error = (e.getMessage() == null) ? e.getClass().getSimpleName() : e.getMessage();
            job.status = Status.ERROR;
        }
    }
}
