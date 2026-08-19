package com.matchiq.vacancy.dto;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
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
public class CreateVacancyRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must have at most 255 characters")
    private String title;

    @NotBlank(message = "description is required")
    private String description;

    @Size(max = 255, message = "company must have at most 255 characters")
    private String company;

    @Size(max = 120, message = "location must have at most 120 characters")
    private String location;

    private WorkModality workModality;

    @Size(max = 100, message = "salaryRange must have at most 100 characters")
    private String salaryRange;

    @Size(max = 500, message = "url must have at most 500 characters")
    private String url;
}
