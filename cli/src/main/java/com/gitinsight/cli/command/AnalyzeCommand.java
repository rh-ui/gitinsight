package com.gitinsight.cli.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.gitinsight.cli.render.JsonReportWriter;
import com.gitinsight.cli.render.ReportFormatter;
import com.gitinsight.core.exception.EmptyRepositoryException;
import com.gitinsight.core.exception.NotAGitRepositoryException;
import com.gitinsight.core.exception.RemoteRepositoryException;
import com.gitinsight.core.service.AnalysisService;
import com.gitinsight.core.service.RepositoryResolver;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "analyze", mixinStandardHelpOptions = true, description = "Analyse un dépôt Git et affiche les métriques.")
public class AnalyzeCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", paramLabel = "REPO", defaultValue = ".", description = "Chemin local ou URL distante (https://....git) du depot. Defaut : dossier courant.")
    String source;

    @Option(names = "--json", description = "Exporte le resultat en JSON (meme schéma que l'API).")
    boolean json;

    @Option(names = { "--top",
            "-n" }, defaultValue = "10", description = "Nombre de hotspots a afficher (defaut : 10).")
    int top;

    @Option(names = "--ascii", description = "Rendu 100%% ASCII (pour les terminaux sans page de code UTF-8).")
    boolean ascii;

    private final AnalysisService service;
    private final ReportFormatter formatter;
    private final JsonReportWriter jsonWriter;

    public AnalyzeCommand() {
        this.service = new AnalysisService();
        this.formatter = new ReportFormatter();
        this.jsonWriter = new JsonReportWriter();
    }

    public AnalyzeCommand(AnalysisService service, ReportFormatter formatter, JsonReportWriter jsonWriter) {
        this.service = service;
        this.formatter = formatter;
        this.jsonWriter = jsonWriter;
    }

    @Override
    public Integer call() {
        if (top <= 0) {
            spec.commandLine().getErr().println("--top doit être > 0 (reçu : " + top + ")");
            return ExitCode.USAGE; // 2 : erreur d'usage
        }

        try {
            var analysis = service.analyze(source, top);

            // Libellé affiché : l'URL telle quelle pour un dépôt distant, sinon le
            // chemin local absolu normalisé (le clone temporaire reste interne).
            String repoLabel = RepositoryResolver.isRemoteUrl(source)
                    ? source
                    : Path.of(source).toAbsolutePath().normalize().toString();

            String output = json
                    ? jsonWriter.toJson(analysis)
                    : formatter.format(analysis, repoLabel, ascii);

            spec.commandLine().getOut().println(output);
            return 0;

        } catch (NotAGitRepositoryException | EmptyRepositoryException | RemoteRepositoryException e) {
            spec.commandLine().getErr().println("Erreur : " + e.getMessage());
            return 1;

        } catch (IOException e) {
            spec.commandLine().getErr().println("Erreur de lecture du depot Git.");
            return 1;
        }
    }
}