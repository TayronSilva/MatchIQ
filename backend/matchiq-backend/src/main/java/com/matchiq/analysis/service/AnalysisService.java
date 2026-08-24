package com.matchiq.analysis.service;

import com.matchiq.analysis.domain.Analysis;
import com.matchiq.analysis.dto.AnalysisResponse;
import com.matchiq.analysis.mapper.AnalysisMapper;
import com.matchiq.analysis.repository.AnalysisRepository;
import com.matchiq.common.ai.AiClient;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.domain.MatchStatus;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MatchRepository matchRepository;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final AnalysisMapper mapper;
    private final MatchMapper matchMapper;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisResponse generate(Long userId, Long matchId) {
        Match match = matchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        List<String> strengths;
        List<String> gaps;
        String narrative;

        String aiRaw = aiClient.chat(buildSystem(), buildUser(match));
        AnalysisResult parsed = parseAi(aiRaw);

        if (parsed != null) {
            strengths = parsed.strengths();
            gaps = parsed.gaps();
            narrative = parsed.narrative();
        } else {
            // fallback mecânico (sem IA disponível)
            strengths = matchMapper.readListForMatch(match.getMatchedSkillsJson());
            gaps = matchMapper.readListForMatch(match.getMissingSkillsJson());
            narrative = buildObservations(match.getScore(), gaps.size());
        }

        Analysis analysis = analysisRepository.findByMatchId(matchId)
                .orElseGet(Analysis::new);

        analysis.setUserId(userId);
        analysis.setMatchId(matchId);
        analysis.setStatus(MatchStatus.PENDING);
        analysisRepository.save(analysis);

        // ... (IA ou fallback) ...

        analysis.setStrengthsJson(mapper.toJson(strengths));
        analysis.setGapsJson(mapper.toJson(gaps));
        analysis.setObservations(narrative);
        analysis.setStatus(MatchStatus.COMPLETED);

        Analysis saved = analysisRepository.save(analysis);
        return mapper.toResponse(saved, match.getScore());
    }

    /**
     * Dispara a geração em background. O MatchService chama este método após calcular
     * o match, e o usuário vê a análise aparecer via polling (status PENDING -> COMPLETED).
     */
    @Async
    @Transactional
    public void generateAsync(Long userId, Long matchId) {
        try {
            generate(userId, matchId);
        } catch (Exception e) {
            log.warn("Geração assíncrona de análise falhou para match {}: {}", matchId, e.getMessage());
            analysisRepository.findByMatchId(matchId).ifPresent(a -> {
                a.setStatus(MatchStatus.FAILED);
                analysisRepository.save(a);
            });
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResponse findByMatchId(Long userId, Long matchId) {
        Analysis analysis = analysisRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for match id: " + matchId));

        if (!analysis.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Analysis not found for match id: " + matchId);
        }

        Match match = matchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        return mapper.toResponse(analysis, match.getScore());
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> findByUserId(Long userId) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(a -> {
                    Match match = matchRepository.findByIdAndUserId(a.getMatchId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + a.getMatchId()));
                    return mapper.toResponse(a, match.getScore());
                })
                .toList();
    }

    private String buildSystem() {
        return """
                Você é um recrutador sênior e mentor de carreira para desenvolvedores de tecnologia.
                Sua tarefa é analisar o currículo de um candidato em relação a uma vaga e devolver
                um diagnóstico ÚTIL e ESPECÍFICO, em português, no formato JSON abaixo.
                Não seja genérico: cite tecnologias e experiências reais mencionadas nos textos.
                """;
    }

    private String buildUser(Match match) {
        Resume resume = resumeRepository.findByIdAndUserId(match.getResumeId(), match.getUserId())
                .orElse(null);
        Vacancy vacancy = vacancyRepository.findByIdAndUserId(match.getVacancyId(), match.getUserId())
                .orElse(null);

        String resumeText = resume != null && resume.getExtractedText() != null ? resume.getExtractedText() : "";
        String vacancyText = vacancy != null && vacancy.getDescription() != null ? vacancy.getDescription() : "";

        return """
                Currículo do candidato (texto extraído):
                %s

                Descrição da vaga:
                %s

                Skills que o sistema já cruzou — presentes no currículo: %s
                Skills que o sistema já cruzou — ausentes no currículo: %s

                Responda SOMENTE com um JSON válido, sem texto extra, neste formato:
                {
                  "strengths": ["ponto forte específico 1", "ponto forte específico 2"],
                  "gaps": ["lacuna específica 1", "lacuna específica 2"],
                  "narrative": "parágrafo curto explicando por que o candidato tem esse nível de aderência à vaga"
                }
                """.formatted(
                resumeText.length() > 4000 ? resumeText.substring(0, 4000) : resumeText,
                vacancyText.length() > 4000 ? vacancyText.substring(0, 4000) : vacancyText,
                match.getMatchedSkillsJson(),
                match.getMissingSkillsJson());
    }

    private AnalysisResult parseAi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String json = extractJson(raw);
            return objectMapper.readValue(json, AnalysisResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJson(String raw) {
        String cleaned = raw.trim();
        // remove cercas de código ```json ... ```
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

    private String buildObservations(int score, int gapCount) {
        if (score >= 80) {
            return "Excelente compatibilidade! Seu currículo atende a maior parte dos requisitos da vaga.";
        }
        if (score >= 50) {
            return "Boa compatibilidade. Faltam " + gapCount + " skill(s) para um aproveitamento ainda melhor.";
        }
        if (score > 0) {
            return "Compatibilidade moderada. Existem " + gapCount + " skill(s) ausentes que podem ser desenvolvidas.";
        }
        return "Compatibilidade baixa. A vaga exige skills que não foram identificadas no seu currículo.";
    }

    public record AnalysisResult(List<String> strengths, List<String> gaps, String narrative) {
    }
}
