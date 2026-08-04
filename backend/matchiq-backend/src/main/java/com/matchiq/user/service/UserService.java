package com.matchiq.user.service;

import com.matchiq.user.domain.User;
import com.matchiq.user.dto.CreateUserRequest;
import com.matchiq.user.dto.UserResponse;
import com.matchiq.user.mapper.UserMapper;
import com.matchiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    public UserResponse create(CreateUserRequest request) {
        // verificando se já existe o email
        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        // mapeando o request para entidade
        User userEntity = mapper.toEntity(request);

        // salvando a entidade no banco de dados atraves do repository
        User savedUser = repository.save(userEntity);

        // mapeia a entidade salva de volta para o response
        return mapper.toResponse(savedUser);
    }
}
