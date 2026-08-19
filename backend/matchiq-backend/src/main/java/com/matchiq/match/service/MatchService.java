package com.matchiq.match.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.dto.MatchResponse;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.repository.ResumeSkillRepository;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final String ALGORITHM_VERSION = "v1";

    private final MatchRepository matchRepository;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final VacancySkillRepository vacancySkillRepository;
    private final MatchMapper mapper;

    @Transactional
    public MatchResponse calculate(Long userId, Long resumeId, Long vacancyId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
        Vacancy vacancy = vacancyRepository.findByIdAndUserId(vacancyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + vacancyId));

        Set<String> resumeSkills = resumeSkillRepository.findByResumeId(resume.getId())
                .stream()
                .map(rs -> rs.getSkillId().toString())
                .collect(Collectors.toSet());

        List<VacancySkill> vacancySkills = vacancySkillRepository.findByVacancyId(vacancy.getId());

        // skills exigidas pela vaga que o candidato possui
        List<String> matched = vacancySkills.stream()
                .filter(vs -> resumeSkills.contains(vs.getSkillId().toString()))
                .map(vs -> vs.getSkillId().toString())
                .toList();

        // skills exigidas pela vaga que o candidato NÃO possui
        List<String> missing = vacancySkills.stream()
                .filter(vs -> !resumeSkills.contains(vs.getSkillId().toString()))
                .map(vs -> vs.getSkillId().toString())
                .toList();

        int score = vacancySkills.isEmpty() ? 0 : Math.round((matched.size() * 100f) / vacancySkills.size());

        // upsert: recalcular o match do mesmo par resume+vaga
        Match match = matchRepository.findByResumeIdAndVacancyId(resumeId, vacancyId)
                .orElseGet(Match::new);

        match.setUserId(userId);
        match.setResumeId(resumeId);
        match.setVacancyId(vacancyId);
        match.setScore(score);
        match.setMatchedSkillsJson(mapper.toJson(matched));
        match.setMissingSkillsJson(mapper.toJson(missing));
        match.setAlgorithmVersion(ALGORITHM_VERSION);

        Match saved = matchRepository.save(match);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findByUserId(Long userId) {
        return matchRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse findByIdAndUserId(Long id, Long userId) {
        Match match = matchRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return mapper.toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findByResumeId(Long userId, Long resumeId) {
        resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
        return matchRepository.findByUserIdAndResumeIdOrderByCreatedAtDesc(userId, resumeId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
