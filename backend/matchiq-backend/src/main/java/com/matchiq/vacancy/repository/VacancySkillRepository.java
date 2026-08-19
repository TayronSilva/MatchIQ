package com.matchiq.vacancy.repository;

import com.matchiq.vacancy.domain.VacancySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VacancySkillRepository extends JpaRepository<VacancySkill, Long> {

    List<VacancySkill> findByVacancyId(Long vacancyId);

    Optional<VacancySkill> findByVacancyIdAndSkillId(Long vacancyId, Long skillId);

    boolean existsByVacancyIdAndSkillId(Long vacancyId, Long skillId);

    void deleteByVacancyIdAndSkillId(Long vacancyId, Long skillId);

    void deleteByVacancyId(Long vacancyId);
}
