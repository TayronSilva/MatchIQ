package com.matchiq.analysis.controller;

import com.matchiq.analysis.dto.AnalysisResponse;
import com.matchiq.analysis.service.AnalysisService;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse generate(Authentication authentication, @RequestParam Long matchId) {
        Long userId = currentUserId(authentication);
        return analysisService.generate(userId, matchId);
    }

    @GetMapping
    public List<AnalysisResponse> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return analysisService.findByUserId(userId);
    }

    @GetMapping("/match/{matchId}")
    public AnalysisResponse findByMatch(Authentication authentication, @PathVariable Long matchId) {
        Long userId = currentUserId(authentication);
        return analysisService.findByMatchId(userId, matchId);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
