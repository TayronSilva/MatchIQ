package com.matchiq.vacancy.dto;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.skill.domain.SkillLevel;
import com.matchiq.vacancy.domain.VacancySource;
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
public class VacancyResponse {
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private String company;

    private String location;

    private WorkModality workModality;

    private String salaryRange;

    private String url;

    private VacancySource source;

    private boolean needsMoreInfo;

    private List<VacancySkillResponse> skills;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VacancySkillResponse {
        private Long skillId;
        private String name;
        private SkillLevel level;
    }
}
