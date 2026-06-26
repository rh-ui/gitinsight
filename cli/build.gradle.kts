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

    // Le BOM aligne toutes les versions Jackson : on ne la déclare qu'une fois ici.
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}