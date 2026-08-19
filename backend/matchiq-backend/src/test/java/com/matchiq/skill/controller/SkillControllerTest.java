package com.matchiq.skill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchiq.common.exception.SkillAlreadyExistsException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.skill.domain.SkillCategory;
import com.matchiq.skill.dto.CreateSkillRequest;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.dto.UpdateSkillRequest;
import com.matchiq.skill.service.SkillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SkillController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SkillService skillService;

    private SkillResponse sampleResponse() {
        SkillResponse response = new SkillResponse();
        response.setId(1L);
        response.setName("Java");
        response.setCategory(SkillCategory.TECHNICAL);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    @Test
    void create_shouldReturn201AndSkill() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");
        request.setCategory(SkillCategory.TECHNICAL);

        when(skillService.create(any(CreateSkillRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.category").value("TECHNICAL"));
    }

    @Test
    void create_shouldReturn400WhenNameBlank() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("");

        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(skillService, never()).create(any());
    }

    @Test
    void create_shouldReturn409WhenSkillExists() throws Exception {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");

        when(skillService.create(any(CreateSkillRequest.class)))
                .thenThrow(new SkillAlreadyExistsException("Skill already exists: Java"));

        mockMvc.perform(post("/api/v1/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findAll_shouldReturnSkills() throws Exception {
        when(skillService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    void findById_shouldReturnSkill() throws Exception {
        when(skillService.findById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/skills/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_shouldReturnUpdatedSkill() throws Exception {
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setName("Java 21");
        request.setCategory(SkillCategory.TECHNICAL);

        SkillResponse updated = sampleResponse();
        updated.setName("Java 21");

        when(skillService.update(eq(1L), any(UpdateSkillRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/skills/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java 21"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(skillService).delete(1L);

        mockMvc.perform(delete("/api/v1/skills/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(skillService, times(1)).delete(1L);
    }
}
