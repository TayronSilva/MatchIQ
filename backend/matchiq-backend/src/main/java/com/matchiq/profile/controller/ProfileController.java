package com.matchiq.profile.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.service.ProfileService;
import com.matchiq.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse create(Authentication authentication,
                                  @Valid @RequestBody CreateProfileRequest request) {
        Long userId = currentUserId(authentication);
        return profileService.create(userId, request);
    }

    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return profileService.findByUserId(userId);
    }

    @PutMapping
    public ProfileResponse update(Authentication authentication,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = currentUserId(authentication);
        return profileService.update(userId, request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadAvatar(Authentication authentication,
                                        @RequestParam("file") MultipartFile file) {
        Long userId = currentUserId(authentication);
        return profileService.uploadAvatar(userId, file);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
