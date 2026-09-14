package com.matchiq.match.service;

import com.matchiq.analysis.repository.AnalysisRepository;
import com.matchiq.analysis.service.AnalysisService;
import com.matchiq.common.ai.AiClient;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.domain.MatchStatus;
import com.matchiq.match.dto.MatchResponse;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.repository.ProfileRepository;
import com.matchiq.recommendation.repository.RecommendationRepository;
import com.matchiq.recommendation.service.RecommendationService;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.repository.ResumeSkillRepository;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final String ALGORITHM_VERSION = "v3-deterministic";

    private final MatchRepository matchRepository;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final VacancySkillRepository vacancySkillRepository;
    private final SkillRepository skillRepository;
    private final ProfileRepository profileRepository;
    private final MatchMapper mapper;
    private final AiClient aiClient;
    private final AnalysisService analysisService;
    private final RecommendationService recommendationService;
    private final AnalysisRepository analysisRepository;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public MatchResponse calculate(Long userId, Long resumeId, Long vacancyId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
        Vacancy vacancy = vacancyRepository.findByIdAndUserId(vacancyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + vacancyId));

        List<ResumeSkill> resumeSkills = resumeSkillRepository.findByResumeId(resume.getId());
        Set<Long> resumeSkillIds = resumeSkills.stream()
                .map(ResumeSkill::getSkillId)
                .collect(Collectors.toSet());

        List<VacancySkill> vacancySkills = vacancySkillRepository.findByVacancyId(vacancy.getId());

        // skills exigidas pela vaga que o candidato possui (por nome)
        List<String> matched = vacancySkills.stream()
                .filter(vs -> resumeSkillIds.contains(vs.getSkillId()))
                .map(vs -> skillName(vs.getSkillId()))
                .toList();

        // skills exigidas pela vaga que o candidato NÃO possui (por nome)
        List<String> missing = vacancySkills.stream()
                .filter(vs -> !resumeSkillIds.contains(vs.getSkillId()))
                .map(vs -> skillName(vs.getSkillId()))
                .toList();

        // Score determinístico 5 componentes
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        Set<Long> matchedSkillIdSet = new HashSet<>(resumeSkillIds);

        ScoringService.ScoreBreakdown breakdown = ScoringService.calculate(
                profile, vacancy, resumeSkills, vacancySkills, matchedSkillIdSet);

        int score = breakdown.total();
        String rationale = breakdown.rationale();

        // upsert: recalcular o match do mesmo par resume+vaga
        Match match = matchRepository.findByResumeIdAndVacancyId(resumeId, vacancyId)
                .orElseGet(Match::new);

        match.setUserId(userId);
        match.setResumeId(resumeId);
        match.setVacancyId(vacancyId);
        match.setScore(score);
        match.setRationale(rationale);
        match.setMatchedSkillsJson(mapper.toJson(matched));
        match.setMissingSkillsJson(mapper.toJson(missing));
        match.setScoreBreakdown(mapper.toJson(Map.of(
                "competencias", breakdown.competencias(),
                "senioridade", breakdown.senioridade(),
                "regiao", breakdown.regiao(),
                "recencia", breakdown.recencia(),
                "preferencias", breakdown.preferencias()
        )));
        match.setAlgorithmVersion(ALGORITHM_VERSION);
        match.setStatus(MatchStatus.COMPLETED);

        Match saved = matchRepository.save(match);

        // Análise e recomendação são geradas em background (não bloqueiam o usuário).
        analysisService.generateAsync(userId, saved.getId());
        recommendationService.generateAsync(userId, saved.getId());

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

    @Transactional
    public void delete(Long id, Long userId) {
        Match match = matchRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        cascadeDelete(match);
        matchRepository.delete(match);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        List<Match> matches = matchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Match match : matches) {
            cascadeDelete(match);
        }
        matchRepository.deleteByUserId(userId);
    }

    private void cascadeDelete(Match match) {
        analysisRepository.deleteByMatchId(match.getId());
        recommendationRepository.deleteByMatchId(match.getId());
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

    private String skillName(Long skillId) {
        return skillRepository.findById(skillId)
                .map(Skill::getName)
                .orElseGet(() -> skillId.toString());
    }
}
