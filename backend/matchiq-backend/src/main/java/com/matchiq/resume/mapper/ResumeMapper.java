package com.matchiq.resume.mapper;

import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.dto.ResumeResponse;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeResponse toResponse(Resume resume) {
        ResumeResponse response = new ResumeResponse();
        response.setId(resume.getId());
        response.setUserId(resume.getUserId());
        response.setFileName(resume.getFileName());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setLanguage(resume.getLanguage());
        response.setVersion(resume.getVersion());
        response.setProcessingStatus(resume.getProcessingStatus());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }
}
