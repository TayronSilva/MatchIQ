package com.matchiq.user.mapper;

import com.matchiq.user.domain.User;
import com.matchiq.user.dto.CreateUserRequest;
import com.matchiq.user.dto.UpdateUserRequest;
import com.matchiq.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setLanguage(request.getLanguage());
        user.setTheme(request.getTheme());
        user.setNotificationEnabled(Boolean.TRUE.equals(request.getNotificationEnabled()));

        return user;
    }

    public void updateEntity(User user, UpdateUserRequest request) {
        user.setName(request.getName());
        user.setLanguage(request.getLanguage());
        user.setTheme(request.getTheme());
        user.setNotificationEnabled(Boolean.TRUE.equals(request.getNotificationEnabled()));
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setLanguage(user.getLanguage());
        response.setTheme(user.getTheme());
        response.setNotificationEnabled(user.isNotificationEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }
}
