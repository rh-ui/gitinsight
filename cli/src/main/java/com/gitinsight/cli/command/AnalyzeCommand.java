package com.gitinsight.cli.command;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.gitinsight.cli.render.JsonReportWriter;
import com.gitinsight.cli.render.ReportFormatter;
import com.gitinsight.core.service.AnalysisService;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "analyze",
        mixinStandardHelpOptions = true,
        description = "Analyse un depot Git et affiche les metriques"
)
public class AnalyzeCommand implements Callable<Integer> {

    @Spec
    CommandLine.Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "PATH", defaultValue = ".", description = "Chemain vers le dépôt Git (defaut : dossier courant)")
    Path path;

    @Option(names = {"--top", "-n"}, defaultValue = "10", description = "Nombre de hotspots à afficher (default : 10)")
    int top;

    private final AnalysisService service;
    private final ReportFormatter formatter;
    private final JsonReportWriter jsonWriter;

    public AnalyzeCommand() { //<--- utiliser réelement par picocli
        this.service = new AnalysisService();
        this.formatter = new ReportFormatter();
        this.jsonWriter = new JsonReportWriter();
    }

    public AnalyzeCommand(AnalysisService service, ReportFormatter formatter, JsonReportWriter jsonWriter) { // <-- pour les tests
        this.service = service;
        this.formatter = formatter;
        this.jsonWriter = jsonWriter;
    }

    @Override
    public Integer call() {
        spec.commandLine().getOut().println("TODO: analyze" + path);
        return 0;
    }

}
