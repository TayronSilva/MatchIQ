package com.matchiq.tailor.controller;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.tailor.dto.CreateSessionRequest;
import com.matchiq.tailor.dto.TailorResponse;
import com.matchiq.tailor.service.TailorService;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tailor")
@RequiredArgsConstructor
@Slf4j
public class TailorController {

    private final TailorService service;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<TailorResponse> create(Authentication authentication,
                                                 @Valid @RequestBody CreateSessionRequest request) {
        Long userId = currentUserId(authentication);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.create(userId, request));
    }

    @GetMapping
    public List<TailorResponse> list(Authentication authentication) {
        return service.findByUserId(currentUserId(authentication));
    }

    @GetMapping("/{id}")
    public TailorResponse get(Authentication authentication, @PathVariable Long id) {
        return service.findByIdAndUserId(currentUserId(authentication), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        service.delete(currentUserId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(Authentication authentication, @PathVariable Long id) {
        byte[] pdf = service.renderPdf(currentUserId(authentication), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private Long currentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email))
                .getId();
    }
}
