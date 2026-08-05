package com.matchiq.profile.controller;

import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.service.ProfileService;
import com.matchiq.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse create(@Valid @RequestBody CreateProfileRequest request,
                                  Authentication authentication) {
        Long userId = currentUserId(authentication);
        return profileService.create(userId, request);
    }

    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return profileService.findByUserId(userId);
    }

    @PutMapping
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest request,
                                  Authentication authentication) {
        Long userId = currentUserId(authentication);
        return profileService.update(userId, request);
    }

    @GetMapping("/{id}")
    public ProfileResponse findById(@PathVariable Long id) {
        return profileService.findById(id);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email).getId();
    }
}
