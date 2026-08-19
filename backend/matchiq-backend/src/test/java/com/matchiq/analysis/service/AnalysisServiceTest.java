package com.matchiq.analysis.service;

import com.matchiq.analysis.domain.Analysis;
import com.matchiq.analysis.dto.AnalysisResponse;
import com.matchiq.analysis.mapper.AnalysisMapper;
import com.matchiq.analysis.repository.AnalysisRepository;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
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
class AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private AnalysisMapper mapper;

    @Mock
    private MatchMapper matchMapper;

    @InjectMocks
    private AnalysisService service;

    private Match match(int score, String matchedJson, String missingJson) {
        Match m = new Match();
        m.setId(1L);
        m.setUserId(1L);
        m.setScore(score);
        m.setMatchedSkillsJson(matchedJson);
        m.setMissingSkillsJson(missingJson);
        return m;
    }

    @Test
    void generate_shouldCreateAnalysisFromMatch() {
        Match match = match(75, "[\"Java\",\"Spring Boot\"]", "[\"AWS\"]");

        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));
        when(matchMapper.readListForMatch("[\"Java\",\"Spring Boot\"]")).thenReturn(List.of("Java", "Spring Boot"));
        when(matchMapper.readListForMatch("[\"AWS\"]")).thenReturn(List.of("AWS"));
        when(analysisRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        Analysis saved = new Analysis();
        saved.setId(1L);
        saved.setUserId(1L);
        saved.setMatchId(1L);
        when(analysisRepository.save(any(Analysis.class))).thenReturn(saved);

        AnalysisResponse response = new AnalysisResponse();
        response.setId(1L);
        response.setScore(75);
        when(mapper.toResponse(saved, 75)).thenReturn(response);

        AnalysisResponse result = service.generate(1L, 1L);

        assertNotNull(result);
        assertEquals(75, result.getScore());
        // as observações devem ser geradas
        verify(analysisRepository).save(argThat(a -> a.getObservations() != null && !a.getObservations().isBlank()));
    }

    @Test
    void generate_shouldThrowWhenMatchNotFound() {
        when(matchRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generate(1L, 99L));
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void generate_shouldUpsertExistingAnalysis() {
        Match match = match(90, "[\"Java\"]", "[]");

        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));
        when(matchMapper.readListForMatch("[\"Java\"]")).thenReturn(List.of("Java"));
        when(matchMapper.readListForMatch("[]")).thenReturn(List.of());

        Analysis existing = new Analysis();
        existing.setId(5L);
        existing.setUserId(1L);
        existing.setMatchId(1L);
        when(analysisRepository.findByMatchId(1L)).thenReturn(Optional.of(existing));
        when(analysisRepository.save(existing)).thenReturn(existing);

        AnalysisResponse response = new AnalysisResponse();
        response.setScore(90);
        when(mapper.toResponse(existing, 90)).thenReturn(response);

        AnalysisResponse result = service.generate(1L, 1L);

        assertEquals(90, result.getScore());
        verify(analysisRepository).save(existing);
    }

    @Test
    void findByMatchId_shouldReturnAnalysis() {
        Analysis analysis = new Analysis();
        analysis.setId(1L);
        analysis.setUserId(1L);
        analysis.setMatchId(1L);

        Match match = match(80, "[]", "[]");

        when(analysisRepository.findByMatchId(1L)).thenReturn(Optional.of(analysis));
        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));

        AnalysisResponse response = new AnalysisResponse();
        response.setScore(80);
        when(mapper.toResponse(analysis, 80)).thenReturn(response);

        AnalysisResponse result = service.findByMatchId(1L, 1L);

        assertEquals(80, result.getScore());
    }

    @Test
    void findByMatchId_shouldThrowWhenAnalysisNotFound() {
        when(analysisRepository.findByMatchId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByMatchId(1L, 99L));
    }

    @Test
    void findByMatchId_shouldThrowWhenNotOwner() {
        Analysis analysis = new Analysis();
        analysis.setId(1L);
        analysis.setUserId(2L); // outro usuário
        analysis.setMatchId(1L);

        when(analysisRepository.findByMatchId(1L)).thenReturn(Optional.of(analysis));

        assertThrows(ResourceNotFoundException.class, () -> service.findByMatchId(1L, 1L));
    }

    @Test
    void findByUserId_shouldReturnAnalyses() {
        Analysis analysis = new Analysis();
        analysis.setId(1L);
        analysis.setUserId(1L);
        analysis.setMatchId(1L);

        Match match = match(70, "[]", "[]");

        when(analysisRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(analysis));
        when(matchRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(match));

        AnalysisResponse response = new AnalysisResponse();
        response.setScore(70);
        when(mapper.toResponse(analysis, 70)).thenReturn(response);

        List<AnalysisResponse> result = service.findByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(70, result.get(0).getScore());
    }
}
