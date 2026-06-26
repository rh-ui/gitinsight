package com.gitinsight.api.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gitinsight.core.exception.NotAGitRepositoryException;
import com.gitinsight.core.model.AnalysisMeta;
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
        given(analysisService.analyze(any(), anyInt()))
                .willReturn(new RepositoryAnalysis(meta, List.of(), List.of(), List.of()));

        mockMvc.perform(post("/api/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\":\"D:/GitInsight\",\"topHotspots\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalCommits").value(3));
    }

    @Test
    void returnsBadRequestWhenPathIsNotARepository() throws Exception {
        given(analysisService.analyze(any(), anyInt()))
                .willThrow(new NotAGitRepositoryException("Aucun dépôt Git trouvé à ce chemin : /undefined/path"));

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
}
