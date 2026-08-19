package com.matchiq.analysis.mapper;

import com.matchiq.analysis.domain.Analysis;
import com.matchiq.analysis.dto.AnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AnalysisMapper {

    private final ObjectMapper objectMapper;

    public AnalysisResponse toResponse(Analysis analysis, Integer score) {
        AnalysisResponse response = new AnalysisResponse();
        response.setId(analysis.getId());
        response.setUserId(analysis.getUserId());
        response.setMatchId(analysis.getMatchId());
        response.setScore(score);
        response.setStrengths(readList(analysis.getStrengthsJson()));
        response.setGaps(readList(analysis.getGapsJson()));
        response.setObservations(analysis.getObservations());
        response.setCreatedAt(analysis.getCreatedAt());
        response.setUpdatedAt(analysis.getUpdatedAt());
        return response;
    }

    public String toJson(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items == null ? List.of() : items);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
