package com.gitinsight.core.metric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gitinsight.core.model.CommitInfo;
import com.gitinsight.core.model.FileChange;
import com.gitinsight.core.model.Hotspot;

public class HotspotCalculator {
    public List<Hotspot> compute(List<CommitInfo> commits, int topN) {
        Map<String, Integer> changeCounts = new HashMap<>();
        Map<String, Set<String>> authorsByFile = new HashMap<>();

        for (CommitInfo c : commits) {
            for (FileChange fc : c.fileChanges()) {
                String path = fc.path();
                changeCounts.merge(path, 1, Integer::sum);
                authorsByFile.computeIfAbsent(path, k -> new HashSet<>())
                        .add(c.authorEmail());
            }
        }

        return changeCounts.keySet().stream()
                .map(path -> {
                    int changes = changeCounts.get(path);
                    int authors = authorsByFile.get(path).size();
                    return new Hotspot(path, changes, authors, (double) changes * authors);
                })
                .sorted(Comparator.comparingDouble(Hotspot::riskScore).reversed())
                .limit(topN)
                .toList();
    }
}
