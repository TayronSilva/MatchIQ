package com.matchiq.profile.dto;

import com.matchiq.profile.domain.Gender;
import com.matchiq.profile.domain.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {

    private Long id;

    private Long userId;

    private String name;

    private String bio;

    private LocalDate birthDate;

    private String country;

    private String city;

    private Gender gender;

    private Gender lookingFor;

    private List<String> interests;

    private String avatarUrl;

    private String occupation;

    private String education;

    private Integer height;

    private boolean verified;

    private Visibility visibility;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
