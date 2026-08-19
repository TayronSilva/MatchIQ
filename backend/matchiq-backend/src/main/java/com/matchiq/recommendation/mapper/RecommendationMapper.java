package com.matchiq.recommendation.mapper;

import com.matchiq.recommendation.domain.Recommendation;
import com.matchiq.recommendation.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendationMapper {

    private final ObjectMapper objectMapper;

    public RecommendationResponse toResponse(Recommendation recommendation) {
        RecommendationResponse response = new RecommendationResponse();
        response.setId(recommendation.getId());
        response.setUserId(recommendation.getUserId());
        response.setMatchId(recommendation.getMatchId());
        response.setSuggestions(readList(recommendation.getSuggestionsJson()));
        response.setStudyPlan(recommendation.getStudyPlan());
        response.setPriority(recommendation.getPriority());
        response.setSource(recommendation.getSource());
        response.setCreatedAt(recommendation.getCreatedAt());
        response.setUpdatedAt(recommendation.getUpdatedAt());
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
