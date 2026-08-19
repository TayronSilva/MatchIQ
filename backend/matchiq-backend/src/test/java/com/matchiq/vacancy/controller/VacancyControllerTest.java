package com.matchiq.vacancy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.service.VacancyScrapeException;
import com.matchiq.vacancy.service.VacancyService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = VacancyController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class VacancyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private VacancyService vacancyService;

    @MockitoBean
    private UserRepository userRepository;

    private VacancyResponse sampleResponse() {
        VacancyResponse response = new VacancyResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setTitle("Desenvolvedor Java");
        response.setDescription("Vaga para Java com Spring Boot");
        response.setCompany("MatchIQ Inc");
        response.setWorkModality(WorkModality.REMOTE);
        response.setSource(VacancySource.MANUAL);
        response.setSkills(List.of());
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
    void create_shouldReturn201AndVacancy() throws Exception {
        CreateVacancyRequest request = new CreateVacancyRequest();
        request.setTitle("Desenvolvedor Java");
        request.setDescription("Vaga para Java com Spring Boot");
        request.setCompany("MatchIQ Inc");

        mockCurrentUser();
        when(vacancyService.create(eq(1L), any(CreateVacancyRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/vacancies")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Desenvolvedor Java"));
    }

    @Test
    void create_shouldReturn400WhenTitleBlank() throws Exception {
        CreateVacancyRequest request = new CreateVacancyRequest();
        request.setTitle("");
        request.setDescription("Vaga para Java");

        mockCurrentUser();

        mockMvc.perform(post("/api/v1/vacancies")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(vacancyService, never()).create(any(), any());
    }

    @Test
    void createFromUrl_shouldReturn201() throws Exception {
        mockCurrentUser();
        when(vacancyService.createFromUrl(eq(1L), eq("https://exemplo.com/vaga"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/vacancies/from-url")
                        .param("url", "https://exemplo.com/vaga")
                        .principal(auth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Desenvolvedor Java"));
    }

    @Test
    void createFromUrl_shouldReturn400WhenScrapeFails() throws Exception {
        mockCurrentUser();
        when(vacancyService.createFromUrl(eq(1L), eq("https://exemplo.com/erro")))
                .thenThrow(new VacancyScrapeException("Não foi possível ler a vaga"));

        mockMvc.perform(post("/api/v1/vacancies/from-url")
                        .param("url", "https://exemplo.com/erro")
                        .principal(auth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_shouldReturnVacancies() throws Exception {
        mockCurrentUser();
        when(vacancyService.findByUserId(1L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/vacancies").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Desenvolvedor Java"));
    }

    @Test
    void findById_shouldReturnVacancy() throws Exception {
        mockCurrentUser();
        when(vacancyService.findByIdAndUserId(1L, 1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/vacancies/{id}", 1L).principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        mockCurrentUser();
        when(vacancyService.findByIdAndUserId(99L, 1L))
                .thenThrow(new ResourceNotFoundException("Vacancy not found with id: 99"));

        mockMvc.perform(get("/api/v1/vacancies/{id}", 99L).principal(auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturnUpdatedVacancy() throws Exception {
        UpdateVacancyRequest request = new UpdateVacancyRequest();
        request.setTitle("Desenvolvedor Java Sênior");
        request.setDescription("Vaga com Java e AWS");

        VacancyResponse updated = sampleResponse();
        updated.setTitle("Desenvolvedor Java Sênior");

        mockCurrentUser();
        when(vacancyService.update(eq(1L), eq(1L), any(UpdateVacancyRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/vacancies/{id}", 1L)
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Desenvolvedor Java Sênior"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockCurrentUser();
        doNothing().when(vacancyService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/vacancies/{id}", 1L).principal(auth()))
                .andExpect(status().isNoContent());

        verify(vacancyService, times(1)).delete(1L, 1L);
    }
}
