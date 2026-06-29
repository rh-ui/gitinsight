package com.gitinsight.cli.render;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gitinsight.cli.helpers.CliFixtures;

import picocli.CommandLine.Help.Ansi;

class ReportFormatterTest {

    /** Caractère d'échappement ANSI (ESC) : préfixe de toute séquence de couleur. */
    private static final String ESC = String.valueOf((char) 27);

    private String report;

    @BeforeEach
    void setUp() {
        // Ansi.OFF injecté → texte brut, sans codes ANSI → assertions stables.
        // ascii=false → rendu Unicode (boîte + barres █).
        var formatter = new ReportFormatter(Ansi.OFF);
        var analysis  = CliFixtures.sampleAnalysis();
        report = formatter.format(analysis, "/repos/gitinsight", false);
    }

    @Test
    void rendersPlainTextWhenAnsiOff() {
        // En mode OFF, aucune séquence d'échappement ANSI ne doit subsister.
        assertThat(report).doesNotContain(ESC);
    }

    @Test
    void headerContainsTotalCommitsAndDateRange() {
        assertThat(report)
            .contains("42")           // totalCommits
            .contains("2026-01-01")   // firstCommit
            .contains("2026-06-01");  // lastCommit
    }

    @Test
    void activeWeekHasLongerBarThanQuietWeek() {
        // On compte les █ ligne par ligne : la 1re barre (semaine active) doit
        // être plus longue que la 2e (semaine calme).
        long activeBlocks = 0, quietBlocks = 0;
        boolean seenFirst = false;
        for (String line : report.split("\n")) {
            if (line.contains("█")) {
                long count = line.chars().filter(c -> c == '█').count();
                if (!seenFirst) { activeBlocks = count; seenFirst = true; }
                else            { quietBlocks  = count; break; }
            }
        }
        assertThat(activeBlocks).isGreaterThan(quietBlocks);
    }

    @Test
    void authorsAppearInReport() {
        assertThat(report)
            .contains("Alice")
            .contains("Bob")
            .contains("30")   // commits d'Alice
            .contains("12");  // commits de Bob
    }

    @Test
    void hotspotsAppearInOrder() {
        int indexService    = report.indexOf("AnalysisService");
        int indexCommitData = report.indexOf("CommitData");
        assertThat(indexService).isLessThan(indexCommitData);
    }

    @Test
    void longPathIsTruncated() {
        // Le chemin complet fait ~60 chars — tronqué par la gauche avec "…".
        assertThat(report)
            .doesNotContain("src/main/java/com/gitinsight/core/service/AnalysisService.java")
            .contains("…");
    }

    @Test
    void asciiModeIsPureAsciiWithFoldedAccents() {
        var asciiReport = new ReportFormatter(Ansi.OFF)
            .format(CliFixtures.sampleAnalysis(), "/repos/gitinsight", true);

        // Aucun octet > 0x7F → s'affiche pareil sur toutes les pages de code.
        assertThat(asciiReport.chars().allMatch(c -> c < 128)).isTrue();
        // Les accents sont pliés : "Vélocité" → "Velocite".
        assertThat(asciiReport).contains("Velocite").doesNotContain("Vélocité");
        // Les glyphes Unicode ont laissé place à l'ASCII.
        assertThat(asciiReport).doesNotContain("█").doesNotContain("─").doesNotContain("╔");
    }
}
