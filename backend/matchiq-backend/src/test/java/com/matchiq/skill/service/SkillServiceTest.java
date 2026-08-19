package com.matchiq.skill.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.common.exception.SkillAlreadyExistsException;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.domain.SkillCategory;
import com.matchiq.skill.dto.CreateSkillRequest;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.dto.UpdateSkillRequest;
import com.matchiq.skill.mapper.SkillMapper;
import com.matchiq.skill.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository repository;

    @Mock
    private SkillMapper mapper;

    @InjectMocks
    private SkillService service;

    private Skill skill;
    private SkillResponse response;

    @BeforeEach
    void setUp() {
        skill = new Skill();
        skill.setId(1L);
        skill.setName("Java");
        skill.setCategory(SkillCategory.TECHNICAL);
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());

        response = new SkillResponse();
        response.setId(1L);
        response.setName("Java");
        response.setCategory(SkillCategory.TECHNICAL);
        response.setCreatedAt(skill.getCreatedAt());
        response.setUpdatedAt(skill.getUpdatedAt());
    }

    @Test
    void create_shouldSaveSkill() {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");
        request.setCategory(SkillCategory.TECHNICAL);

        when(repository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(skill);
        when(repository.save(skill)).thenReturn(skill);
        when(mapper.toResponse(skill)).thenReturn(response);

        SkillResponse result = service.create(request);

        assertNotNull(result);
        assertEquals("Java", result.getName());
        assertEquals(SkillCategory.TECHNICAL, result.getCategory());
        verify(repository).save(skill);
    }

    @Test
    void create_shouldThrowWhenSkillExists() {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");

        when(repository.existsByNameIgnoreCase("Java")).thenReturn(true);

        assertThrows(SkillAlreadyExistsException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void findAll_shouldReturnSkills() {
        when(repository.findAll()).thenReturn(List.of(skill));
        when(mapper.toResponse(skill)).thenReturn(response);

        List<SkillResponse> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getName());
    }

    @Test
    void findById_shouldReturnSkill() {
        when(repository.findById(1L)).thenReturn(Optional.of(skill));
        when(mapper.toResponse(skill)).thenReturn(response);

        SkillResponse result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void update_shouldUpdateSkill() {
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setName("Java 21");
        request.setCategory(SkillCategory.TECHNICAL);

        SkillResponse updatedResponse = new SkillResponse();
        updatedResponse.setId(1L);
        updatedResponse.setName("Java 21");
        updatedResponse.setCategory(SkillCategory.TECHNICAL);

        when(repository.findById(1L)).thenReturn(Optional.of(skill));
        when(repository.save(skill)).thenReturn(skill);
        when(mapper.toResponse(skill)).thenReturn(updatedResponse);

        SkillResponse result = service.update(1L, request);

        assertEquals("Java 21", result.getName());
        verify(mapper).updateEntity(skill, request);
        verify(repository).save(skill);
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setName("Java");

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteSkill() {
        when(repository.findById(1L)).thenReturn(Optional.of(skill));

        service.delete(1L);

        verify(repository).delete(skill);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(repository, never()).delete(any());
    }
}
