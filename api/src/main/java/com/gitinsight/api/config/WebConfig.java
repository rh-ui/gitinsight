package com.gitinsight.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configure CORS pour que le dashboard front (servi par Vite) puisse appeler l'API.
 *
 * Le navigateur applique la « same-origin policy » : une page chargée depuis
 * {@code http://localhost:5173} ne peut pas lire une réponse de
 * {@code http://localhost:8080} sans autorisation explicite du serveur. On déclare
 * donc ici les origines/méthodes autorisées sur {@code /api/**} (le preflight
 * {@code OPTIONS} est inclus).
 *
 * L'origine est externalisée dans la propriété {@code gitinsight.cors.allowed-origins}
 * (défaut : l'origine de dev Vite) pour pouvoir l'ajuster au déploiement sans
 * recompiler.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String allowedOrigins;

    public WebConfig(@Value("${gitinsight.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("POST", "OPTIONS", "GET");
    }
}
