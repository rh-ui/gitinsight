plugins {
    application
}

application {
    mainClass.set("com.gitinsight.cli.GitInsightCommand")
    applicationName = "gitinsight"
}

dependencies {
    implementation(project(":core"))
    implementation("info.picocli:picocli:4.7.6")

    // JGit logue via SLF4J : sans binding, la CLI crache un avertissement NOP à
    // chaque run. On fournit le binding silencieux (un outil n'affiche pas les
    // logs internes de JGit). La version suit celle de slf4j-api tirée par JGit.
    runtimeOnly("org.slf4j:slf4j-nop:1.7.36")

    // Le BOM aligne toutes les versions Jackson : on ne la déclare qu'une fois ici.
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}