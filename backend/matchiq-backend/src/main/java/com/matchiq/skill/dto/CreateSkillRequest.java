package com.matchiq.skill.dto;

import com.matchiq.skill.domain.SkillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateSkillRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must have at most 100 characters")
    private String name;

    private SkillCategory category;
}
