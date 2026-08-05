package com.matchiq.user.service;

import com.matchiq.common.exception.InvalidCredentialsException;
import com.matchiq.config.JwtService;
import com.matchiq.user.domain.User;
import com.matchiq.user.dto.LoginResponse;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(String email, String rawPassword) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Email ou senha incorreto"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Email ou senha incorreto");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new LoginResponse(token);
    }
}
