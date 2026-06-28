package com.gitinsight.cli;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import com.gitinsight.cli.command.AnalyzeCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "gitinsight",
        mixinStandardHelpOptions = true,
        version = "gitinsight 0.1",
        subcommands = {AnalyzeCommand.class},
        description = "Analyse l'historique d'un depot Git"
)
public class GitInsightCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    /** Sans sous-commande : on affiche l'aide sur stderr et on signale une erreur d'usage. */
    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getErr());
        return CommandLine.ExitCode.USAGE;
    }

    public static void main(String[] args) {
        // Sortie UTF-8 déterministe (sinon, sur Windows, la JVM suit la page de code
        // de la console et le rapport part en bouillie). Le rendu --ascii reste, lui,
        // correct quelle que soit la page de code car il n'émet que des octets ≤ 0x7F.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        int exitCode = new CommandLine(new GitInsightCommand()).execute(args);
        System.exit(exitCode);
    }

}
