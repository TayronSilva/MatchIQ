package com.matchiq.profile.mapper;

import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public Profile toEntity(Long userId, CreateProfileRequest request) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setHeadline(request.getHeadline());
        profile.setBio(request.getBio());
        profile.setLocation(request.getLocation());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setAvatarUrl(request.getAvatarUrl());
        return profile;
    }

    public void updateEntity(Profile profile, UpdateProfileRequest request) {
        profile.setHeadline(request.getHeadline());
        profile.setBio(request.getBio());
        profile.setLocation(request.getLocation());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setAvatarUrl(request.getAvatarUrl());
    }

    public ProfileResponse toResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUserId());
        response.setHeadline(profile.getHeadline());
        response.setBio(profile.getBio());
        response.setLocation(profile.getLocation());
        response.setLinkedinUrl(profile.getLinkedinUrl());
        response.setGithubUrl(profile.getGithubUrl());
        response.setPortfolioUrl(profile.getPortfolioUrl());
        response.setAvatarUrl(profile.getAvatarUrl());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
