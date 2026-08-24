package com.matchiq.tailor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    @NotNull
    private Long resumeId;

    @NotNull
    private Long vacancyId;
}
