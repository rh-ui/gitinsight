package com.gitinsight.api.web.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitinsight.core.exception.NotAGitRepositoryException;
import com.gitinsight.core.model.AnalysisMeta;
import com.gitinsight.core.model.FileCoupling;
import com.gitinsight.core.model.FileOwnership;
import com.gitinsight.core.model.RepositoryAnalysis;
import com.gitinsight.core.service.AnalysisService;

@WebMvcTest(AnalyzeController.class)
class AnalyzeControllerTest {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        AnalysisService analysisService;

        @Test
        void returnsAnalysisJsonForValidRequest() throws Exception {
                AnalysisMeta meta = new AnalysisMeta(
                                3,
                                Instant.parse("2024-01-01T00:00:00Z"),
                                Instant.parse("2024-01-10T00:00:00Z"),
                                Instant.parse("2024-02-01T00:00:00Z"));
                given(analysisService.analyze(any(), anyInt())).willReturn(
                                new RepositoryAnalysis(meta, List.of(), List.of(), List.of(), List.of(), List.of()));

                mockMvc.perform(post("/api/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"path\":\"D:/GitInsight\",\"topHotspots\":10}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.meta.totalCommits").value(3));
        }

        @Test
        void returnsBadRequestWhenPathIsNotARepository() throws Exception {
                given(analysisService.analyze(any(), anyInt()))
                                .willThrow(new NotAGitRepositoryException(
                                                "Aucun dépôt Git trouvé à ce chemin : /undefined/path"));

                mockMvc.perform(post("/api/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"path\":\"/undefined/path\",\"topHotspots\":10}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void returnsBadRequestWhenPathIsBlank() throws Exception {
                mockMvc.perform(post("/api/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"path\":\"\",\"topHotspots\":10}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsBadRequestWhenTopHotspotsNotPositive() throws Exception {
                mockMvc.perform(post("/api/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"path\":\"D:/GitInsight\",\"topHotspots\":0}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsBadRequestWhenBodyIsMalformedJson() throws Exception {
                mockMvc.perform(post("/api/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("'{\"path\":\"D:/GitInsight\"}'"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void includesBusFactorAndCouplingInJson() throws Exception {
                AnalysisMeta meta = new AnalysisMeta(
                                1,
                                Instant.parse("2024-01-01T00:00:00Z"),
                                Instant.parse("2024-01-10T00:00:00Z"),
                                Instant.parse("2024-02-01T00:00:00Z"));
                RepositoryAnalysis analysis = new RepositoryAnalysis(
                                meta, List.of(), List.of(), List.of(),
                                List.of(new FileOwnership("A.java", "Alice", "alice@example.com", 10, 10, 1.0)),
                                List.of(new FileCoupling("A.java", "B.java", 3, 4, 3, 0.75)));
                given(analysisService.analyze(any(), anyInt(), anyInt())).willReturn(analysis);

                mockMvc.perform(post("/api/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"path\":\"D:/GitInsight\",\"topHotspots\":10,\"topCoupling\":30}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.busFactor[0].path").value("A.java"))
                                .andExpect(jsonPath("$.busFactor[0].ownership").value(1.0))
                                .andExpect(jsonPath("$.coupling[0].fileA").value("A.java"))
                                .andExpect(jsonPath("$.coupling[0].fileB").value("B.java"))
                                .andExpect(jsonPath("$.coupling[0].couplingScore").value(0.75));
        }
}
