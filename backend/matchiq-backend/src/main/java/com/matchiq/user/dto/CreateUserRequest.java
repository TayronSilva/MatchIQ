package com.matchiq.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must have at most 100 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must have at least 8 characters")
    private String password;

    @Size(max = 20, message = "language must have at most 20 characters")
    private String language;

    @Size(max = 20, message = "theme must have at most 20 characters")
    private String theme;

    private boolean notificationEnabled;
}
