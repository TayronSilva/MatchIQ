package com.matchiq.analysis.service;

import com.matchiq.analysis.domain.Analysis;
import com.matchiq.analysis.dto.AnalysisResponse;
import com.matchiq.analysis.mapper.AnalysisMapper;
import com.matchiq.analysis.repository.AnalysisRepository;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MatchRepository matchRepository;
    private final AnalysisMapper mapper;
    private final MatchMapper matchMapper;

    @Transactional
    public AnalysisResponse generate(Long userId, Long matchId) {
        Match match = matchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        List<String> strengths = matchMapper.readListForMatch(match.getMatchedSkillsJson());
        List<String> gaps = matchMapper.readListForMatch(match.getMissingSkillsJson());

        String observations = buildObservations(match.getScore(), gaps.size());

        Analysis analysis = analysisRepository.findByMatchId(matchId)
                .orElseGet(Analysis::new);

        analysis.setUserId(userId);
        analysis.setMatchId(matchId);
        analysis.setStrengthsJson(mapper.toJson(strengths));
        analysis.setGapsJson(mapper.toJson(gaps));
        analysis.setObservations(observations);

        Analysis saved = analysisRepository.save(analysis);
        return mapper.toResponse(saved, match.getScore());
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
}
