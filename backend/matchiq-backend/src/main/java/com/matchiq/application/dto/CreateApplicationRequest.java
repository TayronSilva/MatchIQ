package com.matchiq.application.dto;

import com.matchiq.application.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    @NotNull
    private Long vacancyId;

    private Long resumeId;

    private Long matchId;

    private ApplicationStatus status;

    private String interviewAt;

    private String notes;

    private List<String> documents;
}
