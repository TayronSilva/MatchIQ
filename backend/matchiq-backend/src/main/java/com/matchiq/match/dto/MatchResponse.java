package com.matchiq.match.dto;

import com.matchiq.match.domain.MatchStatus;
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
public class MatchResponse {
    private Long id;

    private Long userId;

    private Long resumeId;

    private Long vacancyId;

    private Integer score;

    private List<String> matchedSkills;

    private List<String> missingSkills;

    private String algorithmVersion;

    private MatchStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
