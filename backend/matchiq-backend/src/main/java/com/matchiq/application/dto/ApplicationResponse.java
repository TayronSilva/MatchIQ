package com.matchiq.application.dto;

import com.matchiq.application.domain.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Long id;
    private Long userId;
    private Long vacancyId;
    private Long resumeId;
    private Long matchId;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime interviewAt;
    private String notes;
    private List<String> documents;
    private LocalDateTime createdAt;
}
