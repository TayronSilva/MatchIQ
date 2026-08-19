package com.matchiq.skill.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.skill.domain.SkillLevel;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.service.ResumeSkillService;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/skills")
@RequiredArgsConstructor
public class ResumeSkillController {

    private final ResumeSkillService resumeSkillService;
    private final UserRepository userRepository;

    @PostMapping("/{skillId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse addSkill(Authentication authentication,
                                  @PathVariable Long resumeId,
                                  @PathVariable Long skillId,
                                  @RequestParam(value = "level", required = false) SkillLevel level) {
        Long userId = currentUserId(authentication);
        return resumeSkillService.addSkillToResume(resumeId, userId, skillId, level);
    }

    @GetMapping
    public List<SkillResponse> listSkills(Authentication authentication, @PathVariable Long resumeId) {
        Long userId = currentUserId(authentication);
        return resumeSkillService.findSkillsByResume(resumeId, userId);
    }

    @DeleteMapping("/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSkill(Authentication authentication,
                            @PathVariable Long resumeId,
                            @PathVariable Long skillId) {
        Long userId = currentUserId(authentication);
        resumeSkillService.removeSkillFromResume(resumeId, userId, skillId);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
