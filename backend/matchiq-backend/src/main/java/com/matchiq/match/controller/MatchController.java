package com.matchiq.match.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.dto.MatchResponse;
import com.matchiq.match.service.MatchService;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final UserRepository userRepository;

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse calculate(Authentication authentication,
                                   @RequestParam Long resumeId,
                                   @RequestParam Long vacancyId) {
        Long userId = currentUserId(authentication);
        return matchService.calculate(userId, resumeId, vacancyId);
    }

    @GetMapping
    public List<MatchResponse> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return matchService.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public MatchResponse findById(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        return matchService.findByIdAndUserId(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        matchService.delete(id, userId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(Authentication authentication) {
        Long userId = currentUserId(authentication);
        matchService.deleteAllByUserId(userId);
    }

    @GetMapping("/resume/{resumeId}")
    public List<MatchResponse> listByResume(Authentication authentication, @PathVariable Long resumeId) {
        Long userId = currentUserId(authentication);
        return matchService.findByResumeId(userId, resumeId);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
