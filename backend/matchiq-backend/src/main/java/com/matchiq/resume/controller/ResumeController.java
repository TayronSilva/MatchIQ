package com.matchiq.resume.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.resume.dto.ResumeResponse;
import com.matchiq.resume.dto.UpdateResumeRequest;
import com.matchiq.resume.service.ResumeService;
import com.matchiq.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse upload(Authentication authentication,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "language", defaultValue = "pt-BR") String language) {
        Long userId = currentUserId(authentication);
        return resumeService.upload(userId, file, language);
    }

    @GetMapping
    public List<ResumeResponse> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return resumeService.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public ResumeResponse findById(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        return resumeService.findByIdAndUserId(id, userId);
    }

    @PutMapping("/{id}")
    public ResumeResponse updateLanguage(Authentication authentication,
                                         @PathVariable Long id,
                                         @Valid @RequestBody UpdateResumeRequest request) {
        Long userId = currentUserId(authentication);
        return resumeService.updateLanguage(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        resumeService.delete(id, userId);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
