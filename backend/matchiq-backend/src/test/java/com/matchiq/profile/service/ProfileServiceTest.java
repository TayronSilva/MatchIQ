package com.matchiq.profile.service;

import com.matchiq.common.exception.ProfileAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.profile.domain.ProfessionalLevel;
import com.matchiq.profile.domain.WorkModality;
import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.mapper.ProfileMapper;
import com.matchiq.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository repository;

    @Mock
    private ProfileMapper mapper;

    @InjectMocks
    private ProfileService service;

    private Profile profile;
    private ProfileResponse response;

    @BeforeEach
    void setUp() {
        profile = new Profile();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setHeadline("Java Backend Developer");
        profile.setProfessionalLevel(ProfessionalLevel.SENIOR);
        profile.setYearsOfExperience(8);
        profile.setWorkModality(WorkModality.REMOTE);
        profile.setDesiredLocation("São Paulo, Brasil");
        profile.setSalaryExpectation(new BigDecimal("15000.00"));

        response = new ProfileResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setHeadline("Java Backend Developer");
        response.setProfessionalLevel(ProfessionalLevel.SENIOR);
        response.setYearsOfExperience(8);
        response.setWorkModality(WorkModality.REMOTE);
        response.setDesiredLocation("São Paulo, Brasil");
        response.setSalaryExpectation(new BigDecimal("15000.00"));
    }

    @Test
    void create_shouldSaveProfile() {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setHeadline("Java Backend Developer");

        when(repository.existsByUserId(1L)).thenReturn(false);
        when(mapper.toEntity(1L, request)).thenReturn(profile);
        when(repository.save(profile)).thenReturn(profile);
        when(mapper.toResponse(profile)).thenReturn(response);

        ProfileResponse result = service.create(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("Java Backend Developer", result.getHeadline());
        assertEquals(ProfessionalLevel.SENIOR, result.getProfessionalLevel());
        assertEquals(8, result.getYearsOfExperience());
        assertEquals(WorkModality.REMOTE, result.getWorkModality());
        assertEquals("São Paulo, Brasil", result.getDesiredLocation());
        assertEquals(0, new BigDecimal("15000.00").compareTo(result.getSalaryExpectation()));
        verify(repository).existsByUserId(1L);
        verify(repository).save(profile);
    }

    @Test
    void create_shouldThrowWhenProfileAlreadyExists() {
        CreateProfileRequest request = new CreateProfileRequest();
        when(repository.existsByUserId(1L)).thenReturn(true);

        assertThrows(ProfileAlreadyExistsException.class, () -> service.create(1L, request));

        verify(repository, never()).save(any());
    }

    @Test
    void findByUserId_shouldReturnProfile() {
        when(repository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(mapper.toResponse(profile)).thenReturn(response);

        ProfileResponse result = service.findByUserId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
    }

    @Test
    void findByUserId_shouldThrowWhenNotFound() {
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByUserId(99L));
    }

    @Test
    void update_shouldUpdateProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setHeadline("Senior Java Backend Developer");

        ProfileResponse updatedResponse = new ProfileResponse();
        updatedResponse.setId(1L);
        updatedResponse.setUserId(1L);
        updatedResponse.setHeadline("Senior Java Backend Developer");

        when(repository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);
        when(mapper.toResponse(profile)).thenReturn(updatedResponse);

        ProfileResponse result = service.update(1L, request);

        assertNotNull(result);
        assertEquals("Senior Java Backend Developer", result.getHeadline());
        verify(mapper).updateEntity(profile, request);
        verify(repository).save(profile);
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));

        verify(repository, never()).save(any());
    }
}
