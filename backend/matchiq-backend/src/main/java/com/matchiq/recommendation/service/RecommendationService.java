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
            "Você é um mentor de carreira sênior para desenvolvedores de tecnologia brasileiros. " +
            "Gere um plano de estudos prático, objetivo e conciso em português. " +
            "Use linguagem direta, sem enrolação. Cada tópico deve ter nome da skill, " +
            "por que é importante para a vaga, e 1 recurso prático (curso, projeto ou documento oficial). " +
            "Maximo 5 topicos. Nao invente skills que nao estejam na lista de ausentes.";

    @Transactional
    public RecommendationResponse generate(Long userId, Long matchId) {
        Match match = matchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        List<String> gaps = matchMapper.readListForMatch(match.getMissingSkillsJson());
        List<String> suggestions = buildLocalSuggestions(match.getScore(), gaps);

        String aiStudyPlan = null;
        RecommendationSource source = RecommendationSource.LOCAL;

        if (!gaps.isEmpty()) {
            try {
                aiStudyPlan = aiClient.chat(RECOMMENDATION_SYSTEM, buildPrompt(gaps));
            } catch (Exception e) {
                log.warn("AI call for study plan failed for match {}: {}", matchId, e.getMessage());
            }
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
        StringBuilder sb = new StringBuilder("## Plano de Estudos\n\n");
        int i = 1;
        for (String gap : gaps) {
            sb.append("### ").append(i++).append(". ").append(gap).append("\n");
            sb.append("**Por que é importante:** Skill exigida pela vaga e ausente no seu currículo.\n\n");
            sb.append("**Como estudar:** Estude os fundamentos, depois crie um projeto prático para portfólio.\n\n");
        }
        sb.append("---\n\n");
        sb.append("**Dica:** Crie um projeto no GitHub combinando essas skills para demonstrar na prática.");
        return sb.toString();
    }

    private String buildPrompt(List<String> gaps) {
        String gupyGuide = knowledgeBaseService.gupyGuide();
        String knowledgeContext = gupyGuide.isBlank()
                ? ""
                : "\n\nGuia Gupy para referencia:\n" + gupyGuide;

        return """
                Gere um plano de estudos para um desenvolvedor que precisa dominar as seguintes skills para uma vaga:

                Skills ausentes: %s

                Formato obrigatório (em Markdown):
                ## Plano de Estudos
                ### 1. [Nome da Skill]
                **Por que é importante:** 1 frase explicando a relevância para a vaga.
                **Como estudar:** 1-2 frases com recurso prático (curso, projeto, ou doc oficial).

                (repita para cada skill, maximo 5)

                Regras:
                - Seja direto e pratico. Sem textao introdutorio.
                - Cada topico deve ter no maximo 3 linhas.
                - Termine com uma dica final curta sobre portfolio.%s
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
