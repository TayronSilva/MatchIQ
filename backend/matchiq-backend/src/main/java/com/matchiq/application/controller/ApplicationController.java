package com.matchiq.application.controller;

import com.matchiq.application.domain.ApplicationStatus;
import com.matchiq.application.dto.ApplicationResponse;
import com.matchiq.application.dto.CreateApplicationRequest;
import com.matchiq.application.dto.UpdateApplicationRequest;
import com.matchiq.application.service.ApplicationService;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService service;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(Authentication authentication,
                                                      @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(201).body(service.create(currentUserId(authentication), request));
    }

    @GetMapping
    public List<ApplicationResponse> list(Authentication authentication,
                                          @RequestParam(required = false) ApplicationStatus status) {
        return service.findByUserId(currentUserId(authentication), status);
    }

    @GetMapping("/by-vacancy/{vacancyId}")
    public ApplicationResponse byVacancy(Authentication authentication, @PathVariable Long vacancyId) {
        return service.findByVacancy(currentUserId(authentication), vacancyId);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(Authentication authentication, @PathVariable Long id) {
        return service.findByIdAndUserId(currentUserId(authentication), id);
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(Authentication authentication, @PathVariable Long id,
                                      @Valid @RequestBody UpdateApplicationRequest request) {
        return service.update(currentUserId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        service.delete(currentUserId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
