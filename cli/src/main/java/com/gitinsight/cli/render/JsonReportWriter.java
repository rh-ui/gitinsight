package com.gitinsight.cli.render;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gitinsight.core.model.RepositoryAnalysis;

public class JsonReportWriter {

    private final ObjectMapper mapper;

    public JsonReportWriter() {
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String toJson(RepositoryAnalysis analysis) {
        try {
            return mapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Erreur de sérialisation JSON", e);
        }
    }
}