plugins {
    java
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    // Backend SLF4J pour les tests : Logback, cohérent avec Spring Boot dans :api.
    // Évite le conflit "deux providers SLF4J" qui casse le contexte Spring côté api.
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")
}
