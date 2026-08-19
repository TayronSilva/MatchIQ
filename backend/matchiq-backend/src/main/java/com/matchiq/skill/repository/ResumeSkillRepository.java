package com.matchiq.skill.repository;

import com.matchiq.skill.domain.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {

    List<ResumeSkill> findByResumeId(Long resumeId);

    Optional<ResumeSkill> findByResumeIdAndSkillId(Long resumeId, Long skillId);

    boolean existsByResumeIdAndSkillId(Long resumeId, Long skillId);

    void deleteByResumeIdAndSkillId(Long resumeId, Long skillId);
}
