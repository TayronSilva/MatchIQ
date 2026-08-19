package com.matchiq.vacancy.mapper;

import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.dto.VacancyResponse.VacancySkillResponse;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VacancyMapper {

    public Vacancy toEntity(Long userId, CreateVacancyRequest request) {
        Vacancy vacancy = new Vacancy();
        vacancy.setUserId(userId);
        vacancy.setTitle(request.getTitle());
        vacancy.setDescription(request.getDescription());
        vacancy.setCompany(request.getCompany());
        vacancy.setLocation(request.getLocation());
        vacancy.setWorkModality(request.getWorkModality());
        vacancy.setSalaryRange(request.getSalaryRange());
        vacancy.setUrl(request.getUrl());
        vacancy.setSource(request.getUrl() == null || request.getUrl().isBlank() ? VacancySource.MANUAL : VacancySource.URL);
        return vacancy;
    }

    public void updateEntity(Vacancy vacancy, UpdateVacancyRequest request) {
        vacancy.setTitle(request.getTitle());
        vacancy.setDescription(request.getDescription());
        vacancy.setCompany(request.getCompany());
        vacancy.setLocation(request.getLocation());
        vacancy.setWorkModality(request.getWorkModality());
        vacancy.setSalaryRange(request.getSalaryRange());
        vacancy.setUrl(request.getUrl());
    }

    public VacancyResponse toResponse(Vacancy vacancy, List<VacancySkillResponse> skills) {
        VacancyResponse response = new VacancyResponse();
        response.setId(vacancy.getId());
        response.setUserId(vacancy.getUserId());
        response.setTitle(vacancy.getTitle());
        response.setDescription(vacancy.getDescription());
        response.setCompany(vacancy.getCompany());
        response.setLocation(vacancy.getLocation());
        response.setWorkModality(vacancy.getWorkModality());
        response.setSalaryRange(vacancy.getSalaryRange());
        response.setUrl(vacancy.getUrl());
        response.setSource(vacancy.getSource());
        response.setFavorite(vacancy.isFavorite());
        response.setSkills(skills);
        response.setCreatedAt(vacancy.getCreatedAt());
        response.setUpdatedAt(vacancy.getUpdatedAt());
        return response;
    }
}
