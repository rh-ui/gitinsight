package com.gitinsight.cli.command;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.gitinsight.cli.helpers.CliFixtures;
import com.gitinsight.cli.render.JsonReportWriter;
import com.gitinsight.cli.render.ReportFormatter;
import com.gitinsight.core.exception.NotAGitRepositoryException;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.service.AnalysisService;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;

/**
 * Teste la commande de bout en bout SANS dépôt Git : on injecte un
 * {@link AnalysisService} bouchon. On vérifie le routage (texte vs JSON), les
 * flux (stdout/stderr) et les codes de sortie — pas la logique du core, déjà
 * couverte par ses propres tests.
 */
class AnalyzeCommandTest {

    private record Result(int code, String out, String err) {}

    /** Construit la commande avec le service donné, l'exécute et capture les flux. */
    private Result run(AnalysisService service, String... args) {
        var out = new StringWriter();
        var err = new StringWriter();
        // Ansi.OFF → rapport texte brut, assertions stables.
        var command = new AnalyzeCommand(service, new ReportFormatter(Ansi.OFF), new JsonReportWriter());
        int code = new CommandLine(command)
            .setOut(new PrintWriter(out, true))
            .setErr(new PrintWriter(err, true))
            .execute(args);
        return new Result(code, out.toString(), err.toString());
    }

    /** Service bouchon : renvoie une analyse fixe, enregistre le 'top' reçu et l'appel. */
    private static AnalysisService stubReturningSample(int[] capturedTop, boolean[] called) {
        return new AnalysisService() {
            @Override
            public RepositoryAnalysis analyze(String source, int topHotspots) {
                if (called != null) called[0] = true;
                if (capturedTop != null) capturedTop[0] = topHotspots;
                return CliFixtures.sampleAnalysis();
            }
        };
    }

    @Test
    void textReportPrintsSectionsAndExitsZero() {
        var result = run(stubReturningSample(null, null), ".");

        assertThat(result.code()).isZero();
        assertThat(result.out())
            .contains("GitInsight")              // en-tête
            .contains("Vélocité hebdomadaire")   // titre de section (rapport texte)
            .contains("Alice");                  // donnée auteur du fixture
        assertThat(result.err()).isEmpty();
    }

    @Test
    void jsonFlagPrintsJsonNotText() {
        var result = run(stubReturningSample(null, null), ".", "--json");

        assertThat(result.code()).isZero();
        assertThat(result.out())
            .contains("\"meta\"")                    // clés JSON (branche --json)
            .contains("\"velocity\"")
            .doesNotContain("Vélocité hebdomadaire"); // pas le rapport texte
    }

    @Test
    void asciiFlagProducesPureAsciiReport() {
        var result = run(stubReturningSample(null, null), ".", "--ascii");

        assertThat(result.code()).isZero();
        assertThat(result.out().chars().allMatch(c -> c < 128)).isTrue(); // 100% ASCII
        assertThat(result.out()).contains("Velocite");                    // accents pliés
    }

    @Test
    void topOptionIsForwardedToService() {
        int[] captured = {-1};

        var result = run(stubReturningSample(captured, null), ".", "--top", "3");

        assertThat(result.code()).isZero();
        assertThat(captured[0]).isEqualTo(3);
    }

    @Test
    void invalidRepositoryExitsOneWithMessageOnStderr() {
        var failing = new AnalysisService() {
            @Override
            public RepositoryAnalysis analyze(String source, int topHotspots) throws IOException {
                throw new NotAGitRepositoryException("Aucun dépôt Git à ce chemin");
            }
        };

        var result = run(failing, "/chemin/bidon");

        assertThat(result.code()).isEqualTo(1);
        assertThat(result.err()).contains("Aucun dépôt Git à ce chemin");
        assertThat(result.out()).isEmpty();
    }

    @Test
    void nonPositiveTopExitsTwoWithoutCallingService() {
        boolean[] called = {false};

        var result = run(stubReturningSample(null, called), ".", "--top", "0");

        assertThat(result.code()).isEqualTo(2);   // ExitCode.USAGE
        assertThat(result.err()).contains("--top");
        assertThat(called[0]).isFalse();           // la validation court-circuite l'appel au service
    }
}
