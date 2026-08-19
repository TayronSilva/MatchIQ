package com.matchiq.skill.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.domain.SkillLevel;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.mapper.SkillMapper;
import com.matchiq.skill.repository.ResumeSkillRepository;
import com.matchiq.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeSkillService {

    private final ResumeSkillRepository resumeSkillRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final SkillMapper mapper;

    @Transactional
    public SkillResponse addSkillToResume(Long resumeId, Long userId, Long skillId, SkillLevel level) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + skillId));

        if (resumeSkillRepository.existsByResumeIdAndSkillId(resumeId, skillId)) {
            ResumeSkill existing = resumeSkillRepository.findByResumeIdAndSkillId(resumeId, skillId).orElseThrow();
            existing.setLevel(level);
            return mapper.toResponse(resumeSkillRepository.save(existing), skill);
        }

        ResumeSkill resumeSkill = new ResumeSkill();
        resumeSkill.setResumeId(resumeId);
        resumeSkill.setSkillId(skillId);
        resumeSkill.setLevel(level);

        ResumeSkill saved = resumeSkillRepository.save(resumeSkill);
        return mapper.toResponse(saved, skill);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> findSkillsByResume(Long resumeId, Long userId) {
        resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        return resumeSkillRepository.findByResumeId(resumeId)
                .stream()
                .map(rs -> {
                    Skill skill = skillRepository.findById(rs.getSkillId())
                            .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + rs.getSkillId()));
                    return mapper.toResponse(rs, skill);
                })
                .toList();
    }

    @Transactional
    public void removeSkillFromResume(Long resumeId, Long userId, Long skillId) {
        resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        if (!resumeSkillRepository.existsByResumeIdAndSkillId(resumeId, skillId)) {
            throw new ResourceNotFoundException("Skill not linked to resume: " + skillId);
        }

        resumeSkillRepository.deleteByResumeIdAndSkillId(resumeId, skillId);
    }
}
