package com.matchiq.profile.mapper;

import com.matchiq.profile.domain.Gender;
import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.domain.Visibility;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.dto.ProfileResponse;
import com.matchiq.profile.dto.UpdateProfileRequest;
import com.matchiq.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProfileMapper {

    public Profile toEntity(CreateProfileRequest request, User user) {
        Profile profile = new Profile();
        profile.setUser(user);
        applyFields(profile, request.getBio(), request.getBirthDate(), request.getCountry(),
                request.getCity(), request.getGender(), request.getLookingFor(),
                request.getInterests(), request.getAvatarUrl(), request.getOccupation(),
                request.getEducation(), request.getHeight(), request.getVisibility());
        return profile;
    }

    public void updateEntity(Profile profile, UpdateProfileRequest request) {
        applyFields(profile, request.getBio(), request.getBirthDate(), request.getCountry(),
                request.getCity(), request.getGender(), request.getLookingFor(),
                request.getInterests(), request.getAvatarUrl(), request.getOccupation(),
                request.getEducation(), request.getHeight(), request.getVisibility());
    }

    public ProfileResponse toResponse(Profile profile) {
        if (profile == null) {
            return null;
        }

        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setName(profile.getUser().getName());
        response.setBio(profile.getBio());
        response.setBirthDate(profile.getBirthDate());
        response.setCountry(profile.getCountry());
        response.setCity(profile.getCity());
        response.setGender(profile.getGender());
        response.setLookingFor(profile.getLookingFor());
        response.setInterests(profile.getInterests());
        response.setAvatarUrl(profile.getAvatarUrl());
        response.setOccupation(profile.getOccupation());
        response.setEducation(profile.getEducation());
        response.setHeight(profile.getHeight());
        response.setVerified(profile.isVerified());
        response.setVisibility(profile.getVisibility());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        return response;
    }

    private void applyFields(Profile profile, String bio, LocalDate birthDate,
                             String country, String city, Gender gender,
                             Gender lookingFor, List<String> interests, String avatarUrl,
                             String occupation, String education, Integer height,
                             Visibility visibility) {
        profile.setBio(bio);
        profile.setBirthDate(birthDate);
        profile.setCountry(country);
        profile.setCity(city);
        profile.setGender(gender);
        profile.setLookingFor(lookingFor);
        profile.setInterests(interests != null ? new ArrayList<>(interests) : null);
        profile.setAvatarUrl(avatarUrl);
        profile.setOccupation(occupation);
        profile.setEducation(education);
        profile.setHeight(height);
        if (visibility != null) {
            profile.setVisibility(visibility);
        }
    }
}
