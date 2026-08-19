package com.matchiq.skill.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.skill.domain.SkillCategory;
import com.matchiq.skill.domain.SkillLevel;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.service.ResumeSkillService;
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

@WebMvcTest(value = ResumeSkillController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class ResumeSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeSkillService resumeSkillService;

    @MockitoBean
    private UserRepository userRepository;

    private SkillResponse sampleResponse() {
        SkillResponse response = new SkillResponse();
        response.setId(1L);
        response.setName("Java");
        response.setCategory(SkillCategory.TECHNICAL);
        response.setLevel(SkillLevel.ADVANCED);
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
    void addSkill_shouldReturn201AndSkill() throws Exception {
        mockCurrentUser();
        when(resumeSkillService.addSkillToResume(eq(1L), eq(1L), eq(1L), eq(SkillLevel.ADVANCED)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/resumes/{resumeId}/skills/{skillId}", 1L, 1L)
                        .param("level", "ADVANCED")
                        .principal(auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.level").value("ADVANCED"));
    }

    @Test
    void listSkills_shouldReturnSkills() throws Exception {
        mockCurrentUser();
        when(resumeSkillService.findSkillsByResume(1L, 1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/skills", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    void listSkills_shouldReturn404WhenResumeNotFound() throws Exception {
        mockCurrentUser();
        when(resumeSkillService.findSkillsByResume(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Resume not found with id: 99"));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/skills", 99L).principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeSkill_shouldReturn204() throws Exception {
        mockCurrentUser();
        doNothing().when(resumeSkillService).removeSkillFromResume(1L, 1L, 1L);

        mockMvc.perform(delete("/api/v1/resumes/{resumeId}/skills/{skillId}", 1L, 1L).principal(auth()))
                .andExpect(status().isNoContent());

        verify(resumeSkillService, times(1)).removeSkillFromResume(1L, 1L, 1L);
    }
}
