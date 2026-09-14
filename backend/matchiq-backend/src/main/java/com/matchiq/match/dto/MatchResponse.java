package com.matchiq.match.dto;

import com.matchiq.match.domain.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchResponse {
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long vacancyId;

    private Integer score;

    private String rationale;

    private List<String> matchedSkills;

    private List<String> missingSkills;

    private Map<String, Integer> scoreBreakdown;

    private String algorithmVersion;

    private MatchStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
