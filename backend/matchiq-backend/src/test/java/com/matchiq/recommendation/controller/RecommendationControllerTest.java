package com.matchiq.recommendation.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.recommendation.domain.RecommendationPriority;
import com.matchiq.recommendation.domain.RecommendationSource;
import com.matchiq.recommendation.dto.RecommendationResponse;
import com.matchiq.recommendation.service.RecommendationService;
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

@WebMvcTest(value = RecommendationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private UserRepository userRepository;

    private RecommendationResponse sampleResponse() {
        RecommendationResponse response = new RecommendationResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setMatchId(1L);
        response.setSuggestions(List.of("Estude AWS"));
        response.setStudyPlan("1. Estudar AWS...");
        response.setPriority(RecommendationPriority.MEDIUM);
        response.setSource(RecommendationSource.LOCAL);
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
    void generate_shouldReturn201AndRecommendation() throws Exception {
        mockCurrentUser();
        when(recommendationService.generate(eq(1L), eq(1L))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/recommendations/generate")
                        .param("matchId", "1")
                        .principal(auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studyPlan").value("1. Estudar AWS..."))
                .andExpect(jsonPath("$.source").value("LOCAL"));
    }

    @Test
    void generate_shouldReturn404WhenMatchNotFound() throws Exception {
        mockCurrentUser();
        when(recommendationService.generate(eq(1L), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Match not found with id: 99"));

        mockMvc.perform(post("/api/v1/recommendations/generate")
                        .param("matchId", "99")
                        .principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_shouldReturnRecommendations() throws Exception {
        mockCurrentUser();
        when(recommendationService.findByUserId(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/recommendations").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studyPlan").value("1. Estudar AWS..."));
    }

    @Test
    void findByMatch_shouldReturnRecommendation() throws Exception {
        mockCurrentUser();
        when(recommendationService.findByMatchId(1L, 1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/recommendations/match/{matchId}", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(1));
    }
}
