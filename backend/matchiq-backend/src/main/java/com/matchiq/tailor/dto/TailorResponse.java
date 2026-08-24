package com.matchiq.tailor.dto;

import com.matchiq.tailor.domain.TailorStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TailorResponse {

    private Long id;
    private Long userId;
    private Long resumeId;
    private Long vacancyId;
    private TailorStatus status;
    private String contentMarkdown;
    private LocalDateTime createdAt;
}
