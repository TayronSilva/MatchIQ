package com.matchiq.recommendation.dto;

import com.matchiq.recommendation.domain.RecommendationPriority;
import com.matchiq.recommendation.domain.RecommendationSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationResponse {
    private Long id;

    private Long userId;

    private Long matchId;

    private List<String> suggestions;

    private String studyPlan;

    private RecommendationPriority priority;

    private RecommendationSource source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
