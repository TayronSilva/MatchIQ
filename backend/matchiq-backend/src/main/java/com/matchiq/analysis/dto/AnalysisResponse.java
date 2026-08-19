package com.matchiq.analysis.dto;

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
public class AnalysisResponse {
    private Long id;

    private Long userId;

    private Long matchId;

    private Integer score;

    private List<String> strengths;

    private List<String> gaps;

    private String observations;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
