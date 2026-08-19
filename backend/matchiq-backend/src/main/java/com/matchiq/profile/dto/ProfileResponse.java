package com.matchiq.profile.dto;

import com.matchiq.profile.domain.ProfessionalLevel;
import com.matchiq.profile.domain.WorkModality;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long id;

    private Long userId;

    private String headline;

    private String bio;

    private String location;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    private String avatarUrl;

    private ProfessionalLevel professionalLevel;

    private Integer yearsOfExperience;

    private WorkModality workModality;

    private String desiredLocation;

    private BigDecimal salaryExpectation;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
