package com.matchiq.vacancy.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.user.repository.UserRepository;
import com.matchiq.vacancy.collector.CollectOutcome;
import com.matchiq.vacancy.collector.CollectorResult;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.service.VacancyCollectorService;
import com.matchiq.vacancy.service.VacancyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vacancies")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;
    private final VacancyCollectorService vacancyCollectorService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VacancyResponse create(Authentication authentication,
                                  @Valid @RequestBody CreateVacancyRequest request) {
        Long userId = currentUserId(authentication);
        return vacancyService.create(userId, request);
    }

    @PostMapping("/from-url")
    @ResponseStatus(HttpStatus.CREATED)
    public VacancyResponse createFromUrl(Authentication authentication,
                                         @RequestParam("url") @NotBlank @Size(max = 500) String url) {
        Long userId = currentUserId(authentication);
        return vacancyService.createFromUrl(userId, url);
    }

    @GetMapping
    public List<VacancyResponse> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return vacancyService.findByUserId(userId);
    }

    @PostMapping("/collect")
    public ResponseEntity<?> collect(Authentication authentication) {
        Long userId = currentUserId(authentication);
        CollectOutcome outcome = vacancyCollectorService.collectAll(userId);

        if (outcome.cooldownActive()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "message", "Cooldown ativo. Tente novamente em " + outcome.cooldownRemainingMinutes() + " minutos.",
                            "cooldownRemainingMinutes", outcome.cooldownRemainingMinutes()
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "results", outcome.results(),
                "newJobs", outcome.newJobs()
        ));
    }

    @GetMapping("/collect/status")
    public ResponseEntity<?> collectStatus() {
        return ResponseEntity.ok(Map.of(
                "cooldownMinutes", 45,
                "intervalHours", 6,
                "info", "Coleta automática roda a cada 6 horas. POST /api/v1/vacancies/collect para coleta manual."
        ));
    }

    @GetMapping("/{id}")
    public VacancyResponse findById(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        return vacancyService.findByIdAndUserId(id, userId);
    }

    @PutMapping("/{id}")
    public VacancyResponse update(Authentication authentication,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UpdateVacancyRequest request) {
        Long userId = currentUserId(authentication);
        return vacancyService.update(id, userId, request);
    }

    @PutMapping("/{id}/favorite")
    public VacancyResponse favorite(Authentication authentication,
                                    @PathVariable Long id,
                                    @RequestParam boolean favorite) {
        Long userId = currentUserId(authentication);
        return vacancyService.favorite(id, userId, favorite);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        Long userId = currentUserId(authentication);
        vacancyService.delete(id, userId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(Authentication authentication) {
        Long userId = currentUserId(authentication);
        vacancyService.deleteAllByUserId(userId);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
