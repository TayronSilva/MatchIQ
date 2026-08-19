package com.matchiq.vacancy.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.user.repository.UserRepository;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.service.VacancyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vacancies")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;
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

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
