package com.matchiq.tailor.mapper;

import com.matchiq.tailor.domain.ResumeSession;
import com.matchiq.tailor.dto.TailorResponse;
import org.springframework.stereotype.Component;

@Component
public class TailorMapper {

    public TailorResponse toResponse(ResumeSession session) {
        TailorResponse response = new TailorResponse();
        response.setId(session.getId());
        response.setUserId(session.getUserId());
        response.setResumeId(session.getResumeId());
        response.setVacancyId(session.getVacancyId());
        response.setStatus(session.getStatus());
        response.setContentMarkdown(session.getContentMarkdown());
        response.setCreatedAt(session.getCreatedAt());
        return response;
    }
}
