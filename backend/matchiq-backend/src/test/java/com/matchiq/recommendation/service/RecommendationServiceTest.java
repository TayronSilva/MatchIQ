package com.matchiq.recommendation.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.recommendation.domain.Recommendation;
import com.matchiq.recommendation.domain.RecommendationPriority;
import com.matchiq.recommendation.domain.RecommendationSource;
import com.matchiq.recommendation.dto.RecommendationResponse;
import com.matchiq.recommendation.mapper.RecommendationMapper;
import com.matchiq.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private RecommendationMapper mapper;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private HuggingFaceClient huggingFaceClient;

    @InjectMocks
    private RecommendationService service;

    private Match match(int score, String missingJson) {
        Match m = new Match();
        m.setId(1L);
        m.setUserId(1L);
        m.setScore(score);
        m.setMatchedSkillsJson("[]");
        m.setMissingSkillsJson(missingJson);
        return m;
    }

    @Test
    void generate_shouldUseAiWhenAvailable() {
        Match match = match(60, "[\"AWS\"]");

        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));
        when(matchMapper.readListForMatch("[\"AWS\"]")).thenReturn(List.of("AWS"));
        when(huggingFaceClient.generate(anyString())).thenReturn("1. Estudar AWS...\n2. Projeto prático");

        when(recommendationRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        Recommendation saved = new Recommendation();
        saved.setId(1L);
        saved.setUserId(1L);
        saved.setMatchId(1L);
        saved.setSource(RecommendationSource.AI);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved);

        RecommendationResponse response = new RecommendationResponse();
        response.setId(1L);
        response.setSource(RecommendationSource.AI);
        when(mapper.toResponse(saved)).thenReturn(response);

        RecommendationResponse result = service.generate(1L, 1L);

        assertEquals(RecommendationSource.AI, result.getSource());
        verify(huggingFaceClient).generate(anyString());
    }

    @Test
    void generate_shouldFallbackToLocalWhenAiFails() {
        Match match = match(60, "[\"AWS\"]");

        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));
        when(matchMapper.readListForMatch("[\"AWS\"]")).thenReturn(List.of("AWS"));
        when(huggingFaceClient.generate(anyString())).thenReturn(null);

        when(recommendationRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        Recommendation saved = new Recommendation();
        saved.setId(1L);
        saved.setSource(RecommendationSource.LOCAL);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved);

        RecommendationResponse response = new RecommendationResponse();
        response.setSource(RecommendationSource.LOCAL);
        when(mapper.toResponse(saved)).thenReturn(response);

        RecommendationResponse result = service.generate(1L, 1L);

        assertEquals(RecommendationSource.LOCAL, result.getSource());
        // fallback local gera plano de estudos baseado nas lacunas
        verify(recommendationRepository).save(argThat(r ->
                r.getStudyPlan() != null && r.getStudyPlan().contains("AWS")));
    }

    @Test
    void generate_shouldNotCallAiWhenNoGaps() {
        Match match = match(100, "[]");

        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));
        when(matchMapper.readListForMatch("[]")).thenReturn(List.of());
        when(recommendationRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        Recommendation saved = new Recommendation();
        saved.setId(1L);
        saved.setSource(RecommendationSource.LOCAL);
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(saved);

        RecommendationResponse response = new RecommendationResponse();
        response.setSource(RecommendationSource.LOCAL);
        when(mapper.toResponse(saved)).thenReturn(response);

        service.generate(1L, 1L);

        verify(huggingFaceClient, never()).generate(anyString());
        verify(recommendationRepository).save(argThat(r -> r.getPriority() == RecommendationPriority.LOW));
    }

    @Test
    void generate_shouldThrowWhenMatchNotFound() {
        when(matchRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generate(1L, 99L));
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void findByMatchId_shouldReturnRecommendation() {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(1L);
        recommendation.setUserId(1L);
        recommendation.setMatchId(1L);

        when(recommendationRepository.findByMatchId(1L)).thenReturn(Optional.of(recommendation));

        RecommendationResponse response = new RecommendationResponse();
        response.setId(1L);
        when(mapper.toResponse(recommendation)).thenReturn(response);

        RecommendationResponse result = service.findByMatchId(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findByMatchId_shouldThrowWhenNotOwner() {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(1L);
        recommendation.setUserId(2L);
        recommendation.setMatchId(1L);

        when(recommendationRepository.findByMatchId(1L)).thenReturn(Optional.of(recommendation));

        assertThrows(ResourceNotFoundException.class, () -> service.findByMatchId(1L, 1L));
    }
}
