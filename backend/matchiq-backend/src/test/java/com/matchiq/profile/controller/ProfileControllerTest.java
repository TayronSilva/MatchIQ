package com.matchiq.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchiq.common.exception.ProfileAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.service.ProfileService;
import com.matchiq.user.domain.User;
import com.matchiq.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserRepository userRepository;

    private ProfileResponse sampleResponse() {
        ProfileResponse response = new ProfileResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setHeadline("Java Backend Developer");
        response.setBio("Desenvolvedor focado em APIs.");
        response.setLocation("Rio de Janeiro, Brasil");
        response.setLinkedinUrl("https://linkedin.com/in/joao");
        response.setGithubUrl("https://github.com/joao");
        response.setPortfolioUrl("https://portfolio.joao.dev");
        response.setAvatarUrl("https://storage.supabase.co/avatar.png");
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
    void create_shouldReturn201AndProfile() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setHeadline("Java Backend Developer");
        request.setBio("Desenvolvedor focado em APIs.");
        request.setLocation("Rio de Janeiro, Brasil");

        mockCurrentUser();
        when(profileService.create(eq(1L), any(CreateProfileRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/profile")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.headline").value("Java Backend Developer"));
    }

    @Test
    void create_shouldReturn409WhenProfileAlreadyExists() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setHeadline("Java Backend Developer");

        mockCurrentUser();
        when(profileService.create(eq(1L), any(CreateProfileRequest.class)))
                .thenThrow(new ProfileAlreadyExistsException("Profile already exists for user id: 1"));

        mockMvc.perform(post("/api/v1/profile")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400WhenHeadlineTooLong() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setHeadline("a".repeat(121));

        mockCurrentUser();

        mockMvc.perform(post("/api/v1/profile")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).create(any(), any());
    }

    @Test
    void me_shouldReturnProfile() throws Exception {
        mockCurrentUser();
        when(profileService.findByUserId(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/profile/me").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.headline").value("Java Backend Developer"));
    }

    @Test
    void me_shouldReturn404WhenProfileNotFound() throws Exception {
        mockCurrentUser();
        when(profileService.findByUserId(1L))
                .thenThrow(new ResourceNotFoundException("Profile not found for user id: 1"));

        mockMvc.perform(get("/api/v1/profile/me").principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturnUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setHeadline("Senior Java Backend Developer");
        request.setBio("Agora com foco em arquitetura.");

        ProfileResponse updated = sampleResponse();
        updated.setHeadline("Senior Java Backend Developer");

        mockCurrentUser();
        when(profileService.update(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/profile")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Senior Java Backend Developer"));
    }

    @Test
    void update_shouldReturn404WhenProfileNotFound() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setHeadline("Senior Java Backend Developer");

        mockCurrentUser();
        when(profileService.update(eq(1L), any(UpdateProfileRequest.class)))
                .thenThrow(new ResourceNotFoundException("Profile not found for user id: 1"));

        mockMvc.perform(put("/api/v1/profile")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
