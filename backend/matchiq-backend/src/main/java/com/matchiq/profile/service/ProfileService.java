package com.matchiq.profile.service;

import com.matchiq.common.exception.ProfileAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.mapper.ProfileMapper;
import com.matchiq.profile.repository.ProfileRepository;
import com.matchiq.user.domain.User;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper mapper;
    private final SupabaseStorageService storageService;

    @Transactional
    public ProfileResponse create(Long userId, CreateProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException("Profile already exists for user: " + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Profile profile = mapper.toEntity(request, user);
        Profile saved = profileRepository.save(profile);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProfileResponse findById(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + id));
        return mapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse findByUserId(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        return mapper.toResponse(profile);
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        mapper.updateEntity(profile, request);

        Profile updated = profileRepository.save(profile);
        return mapper.toResponse(updated);
    }

    @Transactional
    public ProfileResponse updateAvatar(Long userId, MultipartFile file) {
        validateAvatar(file);

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        String avatarUrl = storageService.uploadAvatar(file);
        profile.setAvatarUrl(avatarUrl);

        Profile updated = profileRepository.save(profile);
        return mapper.toResponse(updated);
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required.");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar file must be at most 5 MB.");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Avatar must be a JPEG, PNG or WebP image.");
        }
    }
}
