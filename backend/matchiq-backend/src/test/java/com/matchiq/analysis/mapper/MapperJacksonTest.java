package com.matchiq.analysis.mapper;

import com.matchiq.analysis.domain.Analysis;
import com.matchiq.analysis.dto.AnalysisResponse;
import com.matchiq.match.mapper.MatchMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa os mappers com o ObjectMapper REAL (Jackson 3) para pegar problemas
 * de serialização que os testes com mock não cobrem.
 */
class MapperJacksonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MatchMapper matchMapper = new MatchMapper(objectMapper);
    private final AnalysisMapper analysisMapper = new AnalysisMapper(objectMapper);

    @Test
    void matchMapper_shouldReadJsonList() {
        List<String> result = matchMapper.readListForMatch("[\"Java\",\"Spring Boot\"]");
        assertEquals(List.of("Java", "Spring Boot"), result);
    }

    @Test
    void matchMapper_shouldWriteJsonList() {
        String json = matchMapper.toJson(List.of("Java", "Spring Boot"));
        assertTrue(json.contains("Java"));
    }

    @Test
    void analysisMapper_shouldRoundTrip() {
        Analysis analysis = new Analysis();
        analysis.setId(1L);
        analysis.setUserId(1L);
        analysis.setMatchId(1L);
        analysis.setStrengthsJson(analysisMapper.toJson(List.of("Java")));
        analysis.setGapsJson(analysisMapper.toJson(List.of("AWS")));
        analysis.setObservations("Boa compatibilidade");

        AnalysisResponse response = analysisMapper.toResponse(analysis, 75);

        assertNotNull(response);
        assertEquals(List.of("Java"), response.getStrengths());
        assertEquals(List.of("AWS"), response.getGaps());
        assertEquals("Boa compatibilidade", response.getObservations());
        assertEquals(75, response.getScore());
    }
}
