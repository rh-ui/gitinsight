pluginManagement {
    plugins {
        id("org.springframework.boot") version "4.1.0"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

plugins {
    // Permet à Gradle de télécharger automatiquement le JDK de la toolchain (Java 21)
    // s'il n'est pas présent sur la machine — indispensable pour une CI reproductible.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "gitinsight"

include("core", "api", "cli")