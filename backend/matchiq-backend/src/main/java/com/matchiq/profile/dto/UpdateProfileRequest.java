package com.matchiq.profile.dto;

import com.matchiq.profile.domain.ProfessionalLevel;
import com.matchiq.profile.domain.WorkModality;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

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

    private ProfessionalLevel professionalLevel;

    @Min(value = 0, message = "yearsOfExperience must be at least 0")
    @Max(value = 50, message = "yearsOfExperience must be at most 50")
    private Integer yearsOfExperience;

    private WorkModality workModality;

    @Size(max = 120, message = "desiredLocation must have at most 120 characters")
    private String desiredLocation;

    @DecimalMin(value = "0.0", message = "salaryExpectation must not be negative")
    private BigDecimal salaryExpectation;
}
