package com.matchiq.application.dto;

import com.matchiq.application.domain.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationRequest {

    private ApplicationStatus status;

    private String interviewAt;

    private String notes;

    private List<String> documents;
}
