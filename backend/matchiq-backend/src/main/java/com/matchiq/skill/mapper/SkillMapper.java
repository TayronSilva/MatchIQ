package com.matchiq.skill.mapper;

import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.domain.SkillLevel;
import com.matchiq.skill.dto.CreateSkillRequest;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.dto.UpdateSkillRequest;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

    public Skill toEntity(CreateSkillRequest request) {
        Skill skill = new Skill();
        skill.setName(request.getName());
        if (request.getCategory() != null) {
            skill.setCategory(request.getCategory());
        }
        return skill;
    }

    public void updateEntity(Skill skill, UpdateSkillRequest request) {
        skill.setName(request.getName());
        if (request.getCategory() != null) {
            skill.setCategory(request.getCategory());
        }
    }

    public SkillResponse toResponse(Skill skill) {
        return toResponse(skill, null);
    }

    public SkillResponse toResponse(Skill skill, SkillLevel level) {
        SkillResponse response = new SkillResponse();
        response.setId(skill.getId());
        response.setName(skill.getName());
        response.setCategory(skill.getCategory());
        response.setLevel(level);
        response.setCreatedAt(skill.getCreatedAt());
        response.setUpdatedAt(skill.getUpdatedAt());
        return response;
    }

    public SkillResponse toResponse(ResumeSkill resumeSkill, Skill skill) {
        return toResponse(skill, resumeSkill.getLevel());
    }
}
