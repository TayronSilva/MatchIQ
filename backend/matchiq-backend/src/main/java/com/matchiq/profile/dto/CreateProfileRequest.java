package com.matchiq.profile.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateProfileRequest {

    @Size(max = 120, message = "headline must have at most 120 characters")
    private String headline;

    @Size(max = 1000, message = "bio must have at most 1000 characters")
    private String bio;

    @Size(max = 120, message = "location must have at most 120 characters")
    private String location;

    @Size(max = 255, message = "linkedinUrl must have at most 255 characters")
    private String linkedinUrl;

    @Size(max = 255, message = "githubUrl must have at most 255 characters")
    private String githubUrl;

    @Size(max = 255, message = "portfolioUrl must have at most 255 characters")
    private String portfolioUrl;

    @Size(max = 255, message = "avatarUrl must have at most 255 characters")
    private String avatarUrl;
}
