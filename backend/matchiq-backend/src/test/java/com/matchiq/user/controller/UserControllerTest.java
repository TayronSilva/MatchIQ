package com.matchiq.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchiq.common.exception.EmailAlreadyExistsException;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.config.JwtAuthenticationFilter;
import com.matchiq.config.SecurityConfig;
import com.matchiq.user.dto.CreateUserRequest;
import com.matchiq.user.dto.UpdateUserRequest;
import com.matchiq.user.dto.UserResponse;
import com.matchiq.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private UserResponse sampleResponse() {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setName("João");
        response.setEmail("joao@email.com");
        response.setLanguage("pt-BR");
        response.setTheme("dark");
        response.setNotificationEnabled(true);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    @Test
    void create_shouldReturn201AndUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("João");
        request.setEmail("joao@email.com");
        request.setPassword("senha12345");
        request.setLanguage("pt-BR");
        request.setTheme("dark");
        request.setNotificationEnabled(true);

        when(userService.create(any(CreateUserRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }

    @Test
    void create_shouldReturn400WhenEmailInvalid() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("João");
        request.setEmail("email-invalido");
        request.setPassword("senha12345");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(any());
    }

    @Test
    void create_shouldReturn400WhenPasswordTooShort() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("João");
        request.setEmail("joao@email.com");
        request.setPassword("123");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(any());
    }

    @Test
    void create_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("João");
        request.setEmail("joao@email.com");
        request.setPassword("senha12345");

        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists."));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findById_shouldReturnUser() throws Exception {
        when(userService.findById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("João"));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/v1/users/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturnUpdatedUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Maria");
        request.setLanguage("en");
        request.setTheme("light");
        request.setNotificationEnabled(false);

        UserResponse updated = sampleResponse();
        updated.setName("Maria");
        when(userService.update(eq(1L), any(UpdateUserRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/v1/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).delete(1L);
    }

    @Test
    void delete_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99")).when(userService).delete(99L);

        mockMvc.perform(delete("/api/v1/users/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
