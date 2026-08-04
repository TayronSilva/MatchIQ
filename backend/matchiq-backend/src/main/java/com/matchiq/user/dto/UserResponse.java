package com.matchiq.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String language;
    private String theme;
    private boolean notificationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
