package com.matchiq.match.mapper;

import com.matchiq.match.domain.Match;
import com.matchiq.match.dto.MatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchMapper {

    private final ObjectMapper objectMapper;

    public MatchResponse toResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setUserId(match.getUserId());
        response.setResumeId(match.getResumeId());
        response.setVacancyId(match.getVacancyId());
        response.setScore(match.getScore());
        response.setMatchedSkills(readList(match.getMatchedSkillsJson()));
        response.setMissingSkills(readList(match.getMissingSkillsJson()));
        response.setAlgorithmVersion(match.getAlgorithmVersion());
        response.setStatus(match.getStatus());
        response.setCreatedAt(match.getCreatedAt());
        response.setUpdatedAt(match.getUpdatedAt());
        return response;
    }

    public String toJson(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills == null ? List.of() : skills);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new tools.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
