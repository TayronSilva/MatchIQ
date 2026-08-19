package com.matchiq.match.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.match.domain.MatchStatus;
import com.matchiq.match.dto.MatchResponse;
import com.matchiq.match.service.MatchService;
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

@WebMvcTest(value = MatchController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private UserRepository userRepository;

    private MatchResponse sampleResponse() {
        MatchResponse response = new MatchResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setResumeId(1L);
        response.setVacancyId(1L);
        response.setScore(80);
        response.setMatchedSkills(List.of("Java", "Spring Boot"));
        response.setMissingSkills(List.of("AWS"));
        response.setAlgorithmVersion("v1");
        response.setStatus(MatchStatus.COMPLETED);
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
    void calculate_shouldReturn201AndMatch() throws Exception {
        mockCurrentUser();
        when(matchService.calculate(eq(1L), eq(1L), eq(1L))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/matches/calculate")
                        .param("resumeId", "1")
                        .param("vacancyId", "1")
                        .principal(auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(80))
                .andExpect(jsonPath("$.matchedSkills[0]").value("Java"))
                .andExpect(jsonPath("$.missingSkills[0]").value("AWS"));
    }

    @Test
    void calculate_shouldReturn404WhenResumeNotFound() throws Exception {
        mockCurrentUser();
        when(matchService.calculate(eq(1L), eq(99L), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Resume not found with id: 99"));

        mockMvc.perform(post("/api/v1/matches/calculate")
                        .param("resumeId", "99")
                        .param("vacancyId", "1")
                        .principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_shouldReturnMatches() throws Exception {
        mockCurrentUser();
        when(matchService.findByUserId(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/matches").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(80));
    }

    @Test
    void findById_shouldReturnMatch() throws Exception {
        mockCurrentUser();
        when(matchService.findByIdAndUserId(1L, 1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/matches/{id}", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        mockCurrentUser();
        when(matchService.findByIdAndUserId(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Match not found with id: 99"));

        mockMvc.perform(get("/api/v1/matches/{id}", 99L).principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listByResume_shouldReturnMatches() throws Exception {
        mockCurrentUser();
        when(matchService.findByResumeId(1L, 1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/matches/resume/{resumeId}", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resumeId").value(1));
    }
}
