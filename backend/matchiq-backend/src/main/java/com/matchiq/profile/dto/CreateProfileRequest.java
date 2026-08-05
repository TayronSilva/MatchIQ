package com.matchiq.profile.dto;

import com.matchiq.profile.domain.Gender;
import com.matchiq.profile.domain.Visibility;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateProfileRequest {

    @Size(max = 500, message = "bio must have at most 500 characters")
    private String bio;

    @Past(message = "birthDate must be in the past")
    private LocalDate birthDate;

    @Size(max = 100, message = "country must have at most 100 characters")
    private String country;

    @Size(max = 100, message = "city must have at most 100 characters")
    private String city;

    private Gender gender;

    private Gender lookingFor;

    @Size(max = 20, message = "interests must have at most 20 items")
    private List<@Size(max = 50, message = "each interest must have at most 50 characters") String> interests;

    @Size(max = 500, message = "avatarUrl must have at most 500 characters")
    private String avatarUrl;

    @Size(max = 100, message = "occupation must have at most 100 characters")
    private String occupation;

    @Size(max = 100, message = "education must have at most 100 characters")
    private String education;

    private Integer height;

    private Visibility visibility;
}
