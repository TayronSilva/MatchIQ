package com.matchiq.skill.dto;

import com.matchiq.skill.domain.SkillCategory;
import com.matchiq.skill.domain.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SkillResponse {
    private Long id;

    private String name;

    private SkillCategory category;

    private SkillLevel level;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
