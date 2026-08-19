package com.matchiq.recommendation.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.recommendation.dto.RecommendationResponse;
import com.matchiq.recommendation.service.RecommendationService;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse generate(Authentication authentication, @RequestParam Long matchId) {
        Long userId = currentUserId(authentication);
        return recommendationService.generate(userId, matchId);
    }

    @GetMapping
    public List<RecommendationResponse> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return recommendationService.findByUserId(userId);
    }

    @GetMapping("/match/{matchId}")
    public RecommendationResponse findByMatch(Authentication authentication, @PathVariable Long matchId) {
        Long userId = currentUserId(authentication);
        return recommendationService.findByMatchId(userId, matchId);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
