package com.matchiq.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.resume.domain.ProcessingStatus;
import com.matchiq.resume.dto.ResumeResponse;
import com.matchiq.resume.dto.UpdateResumeRequest;
import com.matchiq.resume.service.ResumeService;
import com.matchiq.user.domain.User;
import com.matchiq.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ResumeController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ResumeService resumeService;

    @MockitoBean
    private UserRepository userRepository;

    private ResumeResponse sampleResponse() {
        ResumeResponse response = new ResumeResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setFileName("curriculo.pdf");
        response.setFileType("application/pdf");
        response.setFileSize(2048L);
        response.setLanguage("pt-BR");
        response.setVersion(1);
        response.setProcessingStatus(ProcessingStatus.PENDING);
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
    void upload_shouldReturn201AndResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockCurrentUser();
        when(resumeService.upload(eq(1L), any(), eq("pt-BR"))).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(file)
                        .param("language", "pt-BR")
                        .principal(auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("curriculo.pdf"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void list_shouldReturnResumes() throws Exception {
        mockCurrentUser();
        when(resumeService.findByUserId(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/resumes").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("curriculo.pdf"));
    }

    @Test
    void findById_shouldReturnResume() throws Exception {
        mockCurrentUser();
        when(resumeService.findByIdAndUserId(1L, 1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/resumes/{id}", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        mockCurrentUser();
        when(resumeService.findByIdAndUserId(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Resume not found with id: 99"));

        mockMvc.perform(get("/api/v1/resumes/{id}", 99L).principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateLanguage_shouldReturnUpdatedResume() throws Exception {
        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setLanguage("en");

        ResumeResponse updated = sampleResponse();
        updated.setLanguage("en");

        mockCurrentUser();
        when(resumeService.updateLanguage(eq(1L), eq(1L), any(UpdateResumeRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/resumes/{id}", 1L)
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void updateLanguage_shouldReturn400WhenLanguageBlank() throws Exception {
        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setLanguage("");

        mockCurrentUser();

        mockMvc.perform(put("/api/v1/resumes/{id}", 1L)
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resumeService, never()).updateLanguage(any(), any(), any());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockCurrentUser();
        doNothing().when(resumeService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/resumes/{id}", 1L).principal(auth()))
                .andExpect(status().isNoContent());

        verify(resumeService, times(1)).delete(1L, 1L);
    }
}
