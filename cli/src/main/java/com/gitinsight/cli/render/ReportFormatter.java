package com.gitinsight.cli.render;

import java.nio.file.Path;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.gitinsight.core.model.AnalysisMeta;
import com.gitinsight.core.model.AuthorStats;
import com.gitinsight.core.model.Hotspot;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.model.WeeklyVelocity;

import picocli.CommandLine.Help.Ansi;

public class ReportFormatter {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
    private static final int BAR_WIDTH = 20;
    private static final int PATH_MAX  = 45;
    private static final int BOX_WIDTH = 40;

    /** Caractères structurels selon le terminal cible (Unicode joli vs ASCII portable). */
    private enum Glyphs {
        UNICODE("█", "░", "─", "→", "…"),
        ASCII  ("#", ".", "-", "->", "...");

        final String barFull, barEmpty, line, arrow, ellipsis;

        Glyphs(String barFull, String barEmpty, String line, String arrow, String ellipsis) {
            this.barFull  = barFull;
            this.barEmpty = barEmpty;
            this.line     = line;
            this.arrow    = arrow;
            this.ellipsis = ellipsis;
        }
    }

    /** Politique de rendu des couleurs, injectée par l'appelant : AUTO en prod, OFF en test. */
    private final Ansi ansi;

    /** Usage courant : couleurs selon le terminal (respecte les pipes et NO_COLOR). */
    public ReportFormatter() {
        this(Ansi.AUTO);
    }

    public ReportFormatter(Ansi ansi) {
        this.ansi = ansi;
    }

    /**
     * @param ascii {@code true} → rendu 100 % ASCII (boîte, barres et accents pliés)
     *              qui s'affiche correctement sur n'importe quel terminal, même sans
     *              page de code UTF-8 ; {@code false} → rendu Unicode (boîte + barres).
     */
    public String format(RepositoryAnalysis analysis, Path repoPath, boolean ascii) {
        Glyphs g = ascii ? Glyphs.ASCII : Glyphs.UNICODE;
        var sb = new StringBuilder();

        appendHeader(sb, analysis.meta(), repoPath, ascii, g);
        appendVelocity(sb, analysis.velocity(), g);
        appendAuthors(sb, analysis.authors(), g);
        appendHotspots(sb, analysis.hotspots(), g);

        String out = ansi.string(sb.toString());
        // En ASCII, on plie les accents des libellés et des données : la sortie ne
        // contient alors que des octets ≤ 0x7F, identiques dans toutes les pages de code.
        return ascii ? asciiFold(out) : out;
    }

    // ── En-tête ──────────────────────────────────────────────────────────────

    private void appendHeader(StringBuilder sb, AnalysisMeta meta, Path repoPath, boolean ascii, Glyphs g) {
        String title = "GitInsight " + (ascii ? "-" : "—") + " Rapport";
        sb.append("\n");
        if (ascii) {
            sb.append("@|bold,cyan ").append(title).append("|@\n");
            sb.append("@|bold,cyan ").append("=".repeat(title.length())).append("|@\n");
        } else {
            String border = "═".repeat(BOX_WIDTH);
            sb.append("@|bold,cyan ╔").append(border).append("╗|@\n");
            sb.append("@|bold,cyan ║").append(center(title, BOX_WIDTH)).append("║|@\n");
            sb.append("@|bold,cyan ╚").append(border).append("╝|@\n");
        }
        sb.append("\n");
        sb.append(String.format("  @|bold Dépôt    :|@ %s\n", repoPath.toAbsolutePath().normalize()));
        sb.append(String.format("  @|bold Commits  :|@ @|yellow %d|@\n", meta.totalCommits()));
        sb.append(String.format("  @|bold Période  :|@ %s %s %s\n",
            DATE_FMT.format(meta.firstCommit()), g.arrow, DATE_FMT.format(meta.lastCommit())));
        sb.append(String.format("  @|bold Généré   :|@ %s\n", DATE_FMT.format(meta.generatedAt())));
        sb.append("\n");
    }

    // ── Vélocité ─────────────────────────────────────────────────────────────

    private void appendVelocity(StringBuilder sb, List<WeeklyVelocity> velocity, Glyphs g) {
        appendSectionTitle(sb, "Vélocité hebdomadaire", g);

        if (velocity.isEmpty()) {
            sb.append("  Aucune donnée.\n\n");
            return;
        }

        int maxCommits = velocity.stream().mapToInt(WeeklyVelocity::commits).max().orElse(1);

        sb.append(String.format("  %-12s  %-20s  %7s  %8s  %8s  %s\n",
            "Semaine", "Activité", "Commits", "+Lignes", "-Lignes", "Auteurs"));
        sb.append("  ").append(g.line.repeat(75)).append("\n");

        for (var week : velocity) {
            String bar  = buildBar(week.commits(), maxCommits, g);
            String date = DATE_FMT.format(week.weekStart());
            sb.append(String.format("  %-12s  @|green %-20s|@  @|yellow %7d|@  @|green %8d|@  @|red %8d|@  %d\n",
                date, bar, week.commits(),
                week.linesAdded(), week.linesDeleted(), week.activeAuthors()));
        }
        sb.append("\n");
    }

    private String buildBar(int value, int max, Glyphs g) {
        int filled = (max == 0) ? 0 : (int) Math.round((double) value / max * BAR_WIDTH);
        return g.barFull.repeat(filled) + g.barEmpty.repeat(BAR_WIDTH - filled);
    }

    // ── Auteurs ──────────────────────────────────────────────────────────────

    private void appendAuthors(StringBuilder sb, List<AuthorStats> authors, Glyphs g) {
        appendSectionTitle(sb, "Répartition par auteur", g);

        if (authors.isEmpty()) {
            sb.append("  Aucun auteur.\n\n");
            return;
        }

        sb.append(String.format("  %-25s  %8s  %13s  %8s  %8s\n",
            "Auteur", "Commits", "Fichiers", "+Lignes", "-Lignes"));
        sb.append("  ").append(g.line.repeat(70)).append("\n");

        for (var a : authors) {
            sb.append(String.format("  @|bold %-25s|@  @|yellow %8d|@  %13d  @|green %8d|@  @|red %8d|@\n",
                truncate(a.name(), 25, g.ellipsis),
                a.commits(), a.filesTouched(),
                a.linesAdded(), a.linesDeleted()));
        }
        sb.append("\n");
    }

    // ── Hotspots ─────────────────────────────────────────────────────────────

    private void appendHotspots(StringBuilder sb, List<Hotspot> hotspots, Glyphs g) {
        appendSectionTitle(sb, "Fichiers à risque (hotspots)", g);

        if (hotspots.isEmpty()) {
            sb.append("  Aucun hotspot détecté.\n\n");
            return;
        }

        // riskScore = changeCount x auteurs : pas d'échelle absolue (dépend de la
        // taille du repo). On colore donc relativement au max de l'ensemble.
        double maxRisk = hotspots.stream().mapToDouble(Hotspot::riskScore).max().orElse(1);

        sb.append(String.format("  %-45s  %8s  %8s  %9s\n",
            "Fichier", "Commits", "Auteurs", "Risque"));
        sb.append("  ").append(g.line.repeat(75)).append("\n");

        for (var h : hotspots) {
            String path  = truncatePath(h.path(), PATH_MAX, g.ellipsis);
            String risk  = String.format(Locale.ROOT, "%.1f", h.riskScore());
            String color = colorFor(h.riskScore() / maxRisk);
            sb.append(String.format("  %-45s  @|yellow %8d|@  %8d  @|%s %9s|@\n",
                path, h.changeCount(), h.distinctAuthors(), color, risk));
        }
        sb.append("\n");
    }

    private String colorFor(double ratio) {
        if (ratio >= 0.66) return "red";
        if (ratio >= 0.33) return "yellow";
        return "green";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Titre de section : "── Label ────…" (ou "-- Label ----…" en ASCII). */
    private void appendSectionTitle(StringBuilder sb, String label, Glyphs g) {
        int tail = Math.max(2, 52 - label.length());
        sb.append("@|bold,cyan ")
          .append(g.line.repeat(2)).append(' ')
          .append(label).append(' ')
          .append(g.line.repeat(tail))
          .append("|@\n\n");
    }

    /** Centre un texte dans une largeur fixe (titre de la boîte d'en-tête). */
    private String center(String s, int width) {
        int padding = Math.max(0, width - s.length());
        int left = padding / 2;
        return " ".repeat(left) + s + " ".repeat(padding - left);
    }

    /** Tronque un chemin long en gardant la fin (dossier + fichier), la plus informative. */
    String truncatePath(String path, int max, String ellipsis) {
        if (path.length() <= max) return path;
        return ellipsis + path.substring(path.length() - (max - ellipsis.length()));
    }

    private String truncate(String name, int max, String ellipsis) {
        if (name.length() <= max) return name;
        return name.substring(0, max - ellipsis.length()) + ellipsis;
    }

    /** Plie les caractères accentués en ASCII sans toucher aux autres. */
    private static String asciiFold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
