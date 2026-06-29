package com.gitinsight.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gitinsight.api.service.AnalysisJobService;
import com.gitinsight.core.service.AnalysisService;

/**
 * Expose les services du module {@code core} comme beans Spring.
 *
 * On déclare le bean ici plutôt que d'annoter {@code AnalysisService} avec
 * {@code @Service} : cela garde {@code core} totalement indépendant de Spring,
 * tout en permettant l'injection par constructeur dans le contrôleur.
 */
@Configuration
public class CoreConfig {

    @Bean
    AnalysisService analysisService() {
        return new AnalysisService();
    }

    @Bean
    AnalysisJobService analysisJobService(AnalysisService analysisService) {
        return new AnalysisJobService(analysisService);
    }
}
