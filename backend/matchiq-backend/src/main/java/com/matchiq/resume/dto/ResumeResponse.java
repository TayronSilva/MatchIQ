package com.matchiq.resume.dto;

import com.matchiq.resume.domain.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResumeResponse {
    private Long id;

    private Long userId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String language;

    private Integer version;

    private ProcessingStatus processingStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
