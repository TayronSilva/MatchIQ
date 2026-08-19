package com.matchiq.analysis.controller;

import com.matchiq.analysis.dto.AnalysisResponse;
import com.matchiq.analysis.service.AnalysisService;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.user.domain.User;
import com.matchiq.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AnalysisController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private UserRepository userRepository;

    private AnalysisResponse sampleResponse() {
        AnalysisResponse response = new AnalysisResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setMatchId(1L);
        response.setScore(75);
        response.setStrengths(List.of("Java", "Spring Boot"));
        response.setGaps(List.of("AWS"));
        response.setObservations("Boa compatibilidade.");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("joao@email.com", null, java.util.List.of());
    }

    private void mockCurrentUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("joao@email.com");
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
    }

    @Test
    void generate_shouldReturn201AndAnalysis() throws Exception {
        mockCurrentUser();
        when(analysisService.generate(eq(1L), eq(1L))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/analyses/generate")
                        .param("matchId", "1")
                        .principal(auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(75))
                .andExpect(jsonPath("$.strengths[0]").value("Java"))
                .andExpect(jsonPath("$.gaps[0]").value("AWS"));
    }

    @Test
    void generate_shouldReturn404WhenMatchNotFound() throws Exception {
        mockCurrentUser();
        when(analysisService.generate(eq(1L), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Match not found with id: 99"));

        mockMvc.perform(post("/api/v1/analyses/generate")
                        .param("matchId", "99")
                        .principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_shouldReturnAnalyses() throws Exception {
        mockCurrentUser();
        when(analysisService.findByUserId(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/analyses").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(75));
    }

    @Test
    void findByMatch_shouldReturnAnalysis() throws Exception {
        mockCurrentUser();
        when(analysisService.findByMatchId(1L, 1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/analyses/match/{matchId}", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(1));
    }
}
