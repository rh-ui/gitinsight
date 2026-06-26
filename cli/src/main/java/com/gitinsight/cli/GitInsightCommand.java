package com.gitinsight.cli;

import com.gitinsight.cli.command.AnalyzeCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "gitinsight",
        mixinStandardHelpOptions = true,
        version = "gitinsight 0.1",
        subcommands = {AnalyzeCommand.class},
        description = "Analyse l'historique d'un depot Git"
)
class GitInsightCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GitInsightCommand()).execute(args);
        System.exit(exitCode);
    }

}
