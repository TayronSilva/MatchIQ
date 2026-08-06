package com.matchiq.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchiq.common.exception.ProfileAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.profile.domain.Gender;
import com.matchiq.profile.domain.Visibility;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.service.ProfileService;
import com.matchiq.profile.service.SupabaseStorageService;
import com.matchiq.user.dto.UserResponse;
import com.matchiq.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProfileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(ProfileControllerTest.PermissiveSecurityConfig.class)
class ProfileControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class PermissiveSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SupabaseStorageService supabaseStorageService;

    private UserResponse sampleUser() {
        UserResponse user = new UserResponse();
        user.setId(1L);
        user.setName("João");
        user.setEmail("joao@email.com");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private ProfileResponse sampleProfile() {
        ProfileResponse profile = new ProfileResponse();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setName("João");
        profile.setBio("Dev apaixonado por tecnologia");
        profile.setBirthDate(LocalDate.of(1995, 5, 10));
        profile.setCountry("Brasil");
        profile.setCity("São Paulo");
        profile.setGender(Gender.MALE);
        profile.setLookingFor(Gender.FEMALE);
        profile.setInterests(List.of("Java", "Música"));
        profile.setAvatarUrl("https://example.com/avatar.jpg");
        profile.setOccupation("Software Engineer");
        profile.setEducation("Bacharelado em Ciência da Computação");
        profile.setHeight(180);
        profile.setVerified(false);
        profile.setVisibility(Visibility.PUBLIC);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return profile;
    }

    @Test
    void create_shouldReturn201AndProfile() throws Exception {
        when(userService.findByEmail("joao@email.com")).thenReturn(sampleUser());
        when(profileService.create(eq(1L), any(CreateProfileRequest.class))).thenReturn(sampleProfile());

        CreateProfileRequest request = new CreateProfileRequest();
        request.setBio("Dev apaixonado por tecnologia");
        request.setCountry("Brasil");
        request.setCity("São Paulo");

        mockMvc.perform(post("/api/v1/profile")
                        .with(user("joao@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bio").value("Dev apaixonado por tecnologia"));
    }

    @Test
    void create_shouldReturn409WhenProfileAlreadyExists() throws Exception {
        when(userService.findByEmail("joao@email.com")).thenReturn(sampleUser());
        when(profileService.create(eq(1L), any(CreateProfileRequest.class)))
                .thenThrow(new ProfileAlreadyExistsException("Profile already exists for user: 1"));

        CreateProfileRequest request = new CreateProfileRequest();
        request.setBio("Dev");

        mockMvc.perform(post("/api/v1/profile")
                        .with(user("joao@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void me_shouldReturnProfile() throws Exception {
        when(userService.findByEmail("joao@email.com")).thenReturn(sampleUser());
        when(profileService.findByUserId(1L)).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/v1/profile/me").with(user("joao@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.city").value("São Paulo"));
    }

    @Test
    void me_shouldReturn404WhenProfileNotFound() throws Exception {
        when(userService.findByEmail("joao@email.com")).thenReturn(sampleUser());
        when(profileService.findByUserId(1L))
                .thenThrow(new ResourceNotFoundException("Profile not found for user: 1"));

        mockMvc.perform(get("/api/v1/profile/me").with(user("joao@email.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturnUpdatedProfile() throws Exception {
        when(userService.findByEmail("joao@email.com")).thenReturn(sampleUser());

        ProfileResponse updated = sampleProfile();
        updated.setBio("Nova bio");
        when(profileService.update(eq(1L), any(UpdateProfileRequest.class))).thenReturn(updated);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("Nova bio");
        request.setCountry("Portugal");

        mockMvc.perform(put("/api/v1/profile")
                        .with(user("joao@email.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Nova bio"));
    }

    @Test
    void findById_shouldReturnProfile() throws Exception {
        when(profileService.findById(1L)).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/v1/profile/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João"));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        when(profileService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Profile not found with id: 99"));

        mockMvc.perform(get("/api/v1/profile/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadAvatar_shouldReturnProfileWithNewAvatarUrl() throws Exception {
        when(userService.findByEmail("joao@email.com")).thenReturn(sampleUser());

        ProfileResponse updated = sampleProfile();
        updated.setAvatarUrl("https://example.supabase.co/storage/v1/object/public/avatars/avatar.jpg");
        when(profileService.updateAvatar(eq(1L), any(MockMultipartFile.class))).thenReturn(updated);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/profile/avatar")
                        .file(file)
                        .with(user("joao@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://example.supabase.co/storage/v1/object/public/avatars/avatar.jpg"));
    }
}
