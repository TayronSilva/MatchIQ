package com.matchiq.match.service;

import com.matchiq.analysis.service.AnalysisService;
import com.matchiq.common.ai.AiClient;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.domain.MatchStatus;
import com.matchiq.match.dto.MatchResponse;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final String ALGORITHM_VERSION = "v2-ai";

    private final MatchRepository matchRepository;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final VacancySkillRepository vacancySkillRepository;
    private final SkillRepository skillRepository;
    private final MatchMapper mapper;
    private final AiClient aiClient;
    private final AnalysisService analysisService;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MatchResponse calculate(Long userId, Long resumeId, Long vacancyId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
        Vacancy vacancy = vacancyRepository.findByIdAndUserId(vacancyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + vacancyId));

        Set<Long> resumeSkillIds = resumeSkillRepository.findByResumeId(resume.getId())
                .stream()
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

        int mechanicalScore = vacancySkills.isEmpty() ? 0 : Math.round((matched.size() * 100f) / vacancySkills.size());

        // Score semântico pela IA (sinônimos, pesos, contexto). Fallback mecânico se a IA falhar.
        int score = mechanicalScore;
        String rationale = null;
        String aiRaw = aiClient.chat(buildScoreSystem(), buildScoreUser(resume, vacancy, matched, missing));
        MatchScoreResult ai = parseScore(aiRaw);
        if (ai != null) {
            score = ai.score();
            rationale = ai.rationale();
        }

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

    @Transactional(readOnly = true)
    public List<MatchResponse> findByResumeId(Long userId, Long resumeId) {
        resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
        return matchRepository.findByUserIdAndResumeIdOrderByCreatedAtDesc(userId, resumeId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private String buildScoreSystem() {
        return """
                Você é um recrutador sênior. Sua tarefa é dar um score de compatibilidade de 0 a 100
                entre um currículo e uma vaga de tecnologia, considerando sinônimos (ex: "JS" = "JavaScript",
                "Spring" = "Spring Boot") e a importância relativa das skills. Responda SOMENTE com JSON.
                """;
    }

    private String buildScoreUser(Resume resume, Vacancy vacancy, List<String> matched, List<String> missing) {
        String resumeText = resume.getExtractedText() != null ? resume.getExtractedText() : "";
        String vacancyText = vacancy.getDescription() != null ? vacancy.getDescription() : "";
        return """
                Currículo (texto extraído):
                %s

                Descrição da vaga:
                %s

                Skills presentes no currículo (cruzamento mecânico): %s
                Skills ausentes no currículo (cruzamento mecânico): %s

                Responda SOMENTE com um JSON válido, sem texto extra:
                {"score": <número 0-100>, "rationale": "explicação curta do score"}
                """.formatted(
                resumeText.length() > 4000 ? resumeText.substring(0, 4000) : resumeText,
                vacancyText.length() > 4000 ? vacancyText.substring(0, 4000) : vacancyText,
                matched,
                missing);
    }

    private MatchScoreResult parseScore(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String json = extractJson(raw);
            MatchScoreResult result = objectMapper.readValue(json, MatchScoreResult.class);
            if (result.score() < 0 || result.score() > 100) {
                return null;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJson(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*", "").replaceAll("```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            return raw;
        }
        return cleaned.substring(start, end + 1);
    }

    private String skillName(Long skillId) {
        return skillRepository.findById(skillId)
                .map(Skill::getName)
                .orElseGet(() -> skillId.toString());
    }

    public record MatchScoreResult(int score, String rationale) {
    }
}
