package com.gitinsight.cli.render;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitinsight.cli.helpers.CliFixtures;

class JsonReportWriterTest {

    private JsonNode root;

    @BeforeEach
    void setUp() throws Exception {
        var writer = new JsonReportWriter();
        String json = writer.toJson(CliFixtures.sampleAnalysis());

        root = new ObjectMapper().readTree(json);
    }

    @Test
    void jsonIsValidAndHasTopLevelKeys() {
        assertThat(root.has("meta")).isTrue();
        assertThat(root.has("velocity")).isTrue();
        assertThat(root.has("authors")).isTrue();
        assertThat(root.has("hotspots")).isTrue();
    }

    @Test
    void datesAreIsoStringsNotTimestamps() {
        String firstCommit = root.path("meta").path("firstCommit").asText();
        assertThat(firstCommit).startsWith("2026-01-01");
    }

    @Test
    void totalCommitsIsCorrect() {
        int total = root.path("meta").path("totalCommits").asInt();
        assertThat(total).isEqualTo(42);
    }

    @Test
    void velocityArrayIsNotEmpty() {
        assertThat(root.path("velocity").isArray()).isTrue();
        assertThat(root.path("velocity").size()).isGreaterThan(0);
    }
}