package com.matchiq.profile.service;

import com.matchiq.common.exception.ProfileAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.mapper.ProfileMapper;
import com.matchiq.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository repository;
    private final ProfileMapper mapper;

    @Transactional
    public ProfileResponse create(Long userId, CreateProfileRequest request) {
        if (repository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException("Profile already exists for user id: " + userId);
        }

        Profile profile = mapper.toEntity(userId, request);
        Profile saved = repository.save(profile);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProfileResponse findByUserId(Long userId) {
        Profile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
        return mapper.toResponse(profile);
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest request) {
        Profile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));

        mapper.updateEntity(profile, request);

        Profile updated = repository.save(profile);
        return mapper.toResponse(updated);
    }
}
