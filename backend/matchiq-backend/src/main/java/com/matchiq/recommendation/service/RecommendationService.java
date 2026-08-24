package com.matchiq.recommendation.service;

import com.matchiq.common.ai.AiClient;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.domain.MatchStatus;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.recommendation.domain.Recommendation;
import com.matchiq.recommendation.domain.RecommendationPriority;
import com.matchiq.recommendation.domain.RecommendationSource;
import com.matchiq.recommendation.dto.RecommendationResponse;
import com.matchiq.recommendation.mapper.RecommendationMapper;
import com.matchiq.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final MatchRepository matchRepository;
    private final RecommendationMapper mapper;
    private final MatchMapper matchMapper;
    private final AiClient aiClient;
    private final KnowledgeBaseService knowledgeBaseService;

    private static final String RECOMMENDATION_SYSTEM =
            "Você é um mentor de carreira para desenvolvedores. Gere um plano de estudos prático, objetivo e conciso em português.";

    @Transactional
    public RecommendationResponse generate(Long userId, Long matchId) {
        Match match = matchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        List<String> gaps = matchMapper.readListForMatch(match.getMissingSkillsJson());
        List<String> suggestions = buildLocalSuggestions(match.getScore(), gaps);

        String aiStudyPlan = null;
        RecommendationSource source = RecommendationSource.LOCAL;

        if (!gaps.isEmpty()) {
            aiStudyPlan = aiClient.chat(RECOMMENDATION_SYSTEM, buildPrompt(gaps));
            if (aiStudyPlan != null && !aiStudyPlan.isBlank()) {
                source = RecommendationSource.AI;
            }
        }

        Recommendation recommendation = recommendationRepository.findByMatchId(matchId)
                .orElseGet(Recommendation::new);

        recommendation.setUserId(userId);
        recommendation.setMatchId(matchId);
        recommendation.setStatus(MatchStatus.PENDING);
        recommendationRepository.save(recommendation);

        recommendation.setSuggestionsJson(mapper.toJson(suggestions));
        recommendation.setStudyPlan(aiStudyPlan == null || aiStudyPlan.isBlank() ? buildLocalStudyPlan(gaps) : aiStudyPlan);
        recommendation.setPriority(priorityFor(match.getScore()));
        recommendation.setSource(source);
        recommendation.setStatus(MatchStatus.COMPLETED);

        Recommendation saved = recommendationRepository.save(recommendation);
        return mapper.toResponse(saved);
    }

    /**
     * Dispara a geração em background; o frontend faz polling do status.
     */
    @Async
    @Transactional
    public void generateAsync(Long userId, Long matchId) {
        try {
            generate(userId, matchId);
        } catch (Exception e) {
            log.warn("Geração assíncrona de recomendação falhou para match {}: {}", matchId, e.getMessage());
            recommendationRepository.findByMatchId(matchId).ifPresent(r -> {
                r.setStatus(MatchStatus.FAILED);
                recommendationRepository.save(r);
            });
        }
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> findByUserId(Long userId) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse findByMatchId(Long userId, Long matchId) {
        Recommendation recommendation = recommendationRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found for match id: " + matchId));
        if (!recommendation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Recommendation not found for match id: " + matchId);
        }
        return mapper.toResponse(recommendation);
    }

    private List<String> buildLocalSuggestions(int score, List<String> gaps) {
        if (gaps.isEmpty()) {
            return List.of("Seu currículo já atende todas as skills exigidas pela vaga. Continue assim!");
        }
        return List.of(
                "Foque em desenvolver as skills ausentes: " + String.join(", ", gaps) + ".",
                "Atualize seu currículo destacando projetos práticos que usem essas skills."
        );
    }

    private String buildLocalStudyPlan(List<String> gaps) {
        if (gaps.isEmpty()) {
            return "Nenhum plano de estudos necessário: você já domina as skills exigidas.";
        }
        StringBuilder sb = new StringBuilder("Plano de estudos sugerido (ordem recomendada):\n");
        int i = 1;
        for (String gap : gaps) {
            sb.append(i++).append(". Estudar ").append(gap).append(" — comece com fundamentos, depois pratique com projetos.\n");
        }
        sb.append("Dica: crie um projeto de portfólio combinando as skills acima para comprovar na prática.");
        return sb.toString();
    }

    private String buildPrompt(List<String> gaps) {
        String gupyGuide = knowledgeBaseService.gupyGuide();
        String knowledgeContext = gupyGuide.isBlank()
                ? ""
                : "\n\nUse este guia como referência para deixar o plano alinhado ao que a IA da Gupy valoriza em 2026:\n" + gupyGuide;

        return """
                Com base nas skills ausentes no currículo do candidato, gere um plano de estudos CURTO e prático em português.
                Skills ausentes: %s

                Regras obrigatórias:
                - No máximo 5 tópicos, um por skill em falta.
                - Cada tópico: 1 linha de foco + no máximo 1 recurso (nome ou link curto).
                - Nunca deixe frases pela metade; termine sempre com ponto final.
                - Sem introduções, nem resumo final longo.%s
                """.formatted(String.join(", ", gaps), knowledgeContext);
    }

    private RecommendationPriority priorityFor(int score) {
        if (score < 50) {
            return RecommendationPriority.HIGH;
        }
        if (score < 80) {
            return RecommendationPriority.MEDIUM;
        }
        return RecommendationPriority.LOW;
    }
}
