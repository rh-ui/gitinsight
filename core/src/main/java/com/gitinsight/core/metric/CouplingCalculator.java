package com.gitinsight.core.metric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;
import com.gitinsight.core.model.FileCoupling;

/**
 * Calcule le couplage temporel : quels fichiers changent ensemble dans
 * l'historique. Pur (pas de Git), testable sur des {@link CommitInfo}
 * fabriqués.
 */
public class CouplingCalculator {

    /**
     * Clé canonique d'une paire ({@code fileA < fileB} garanti par construction).
     */
    private record Pair(String fileA, String fileB) {
    }

    /**
     * @param commits           historique source.
     * @param topN              nombre maximum de paires retournées.
     * @param maxFilesPerCommit au-delà, le commit ne contribue PAS au pairage
     *                          (merges/commits massifs = bruit + coût O(f²)).
     * @return les paires les plus couplées, score Jaccard décroissant.
     */
    public List<FileCoupling> compute(List<CommitInfo> commits, int topN, int maxFilesPerCommit) {
        Map<String, Integer> changesPerFile = new HashMap<>();
        Map<Pair, Integer> pairCounts = new HashMap<>();

        for (CommitInfo commit : commits) {
            // Chemins distincts du commit, triés → l'ordre canonique des paires est
            // gratuit.
            List<String> files = commit.fileChanges().stream()
                    .map(FileChange::path)
                    .distinct()
                    .sorted()
                    .toList();

            // Toujours compter la fréquence par fichier (même pour les commits massifs).
            for (String file : files) {
                changesPerFile.merge(file, 1, Integer::sum);
            }

            // Skip du pairage pour les commits trop larges (bruit + explosion
            // combinatoire).
            if (files.size() > maxFilesPerCommit) {
                continue;
            }

            for (int i = 0; i < files.size(); i++) {
                for (int j = i + 1; j < files.size(); j++) {
                    pairCounts.merge(new Pair(files.get(i), files.get(j)), 1, Integer::sum);
                }
            }
        }

        return pairCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2) // filtre le bruit (co-change unique)
                .map(entry -> {
                    Pair pair = entry.getKey();
                    int coChanges = entry.getValue();
                    int changesA = changesPerFile.get(pair.fileA());
                    int changesB = changesPerFile.get(pair.fileB());
                    double jaccard = (double) coChanges / (changesA + changesB - coChanges);
                    return new FileCoupling(pair.fileA(), pair.fileB(),
                            coChanges, changesA, changesB, jaccard);
                })
                .sorted(Comparator.comparingDouble(FileCoupling::couplingScore).reversed()
                        .thenComparing(Comparator.comparingInt(FileCoupling::coChanges).reversed())
                        .thenComparing(FileCoupling::fileA)
                        .thenComparing(FileCoupling::fileB))
                .limit(topN)
                .toList();
    }
}