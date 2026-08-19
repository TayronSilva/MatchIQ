package com.matchiq.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateResumeRequest {

    @NotBlank(message = "language is required")
    @Size(max = 20, message = "language must have at most 20 characters")
    private String language;
}
