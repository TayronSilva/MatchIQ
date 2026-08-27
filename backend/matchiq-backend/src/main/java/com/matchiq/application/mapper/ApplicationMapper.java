package com.matchiq.application.mapper;

import com.matchiq.application.domain.Application;
import com.matchiq.application.dto.ApplicationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApplicationMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApplicationResponse toResponse(Application a) {
        ApplicationResponse r = new ApplicationResponse();
        r.setId(a.getId());
        r.setUserId(a.getUserId());
        r.setVacancyId(a.getVacancyId());
        r.setResumeId(a.getResumeId());
        r.setMatchId(a.getMatchId());
        r.setStatus(a.getStatus());
        r.setAppliedAt(a.getAppliedAt());
        r.setInterviewAt(a.getInterviewAt());
        r.setNotes(a.getNotes());
        r.setDocuments(parseDocs(a.getDocumentsJson()));
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }

    public List<String> parseDocs(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String toJson(List<String> docs) {
        if (docs == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(docs);
        } catch (Exception e) {
            return "[]";
        }
    }
}
