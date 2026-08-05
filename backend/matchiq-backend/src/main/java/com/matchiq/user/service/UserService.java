package com.matchiq.user.service;

import com.matchiq.common.exception.EmailAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.user.domain.User;
import com.matchiq.user.dto.CreateUserRequest;
import com.matchiq.user.dto.UpdateUserRequest;
import com.matchiq.user.dto.UserResponse;
import com.matchiq.user.mapper.UserMapper;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        request.setPassword(hashedPassword);

        User userEntity = mapper.toEntity(request);
        User savedUser = repository.save(userEntity);
        return mapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        mapper.updateEntity(user, request);

        User updatedUser = repository.save(user);
        return mapper.toResponse(updatedUser);
    }

    @Transactional
    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        repository.delete(user);
    }

    public boolean verifyLogin(String email, String rawPassword) {
        return repository.findByEmail(email)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(false);
    }
 }
