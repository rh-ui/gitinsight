package com.gitinsight.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gitinsight.core.service.AnalysisService;

/**
 * Expose les services du module {@code core} comme beans Spring.
 *
 * <p>
 * On déclare le bean ici plutôt que d'annoter {@code AnalysisService} avec
 * {@code @Service} : cela garde {@code core} totalement indépendant de Spring
 * (principe du plan — moteur pur, testable sans framework), tout en permettant
 * l'injection par constructeur dans le contrôleur (et le mock en test).
 */
@Configuration
public class CoreConfig {

    @Bean
    AnalysisService analysisService() {
        return new AnalysisService();
    }
}
