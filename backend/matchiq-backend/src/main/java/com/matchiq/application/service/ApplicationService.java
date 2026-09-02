package com.matchiq.application.service;

import com.matchiq.application.domain.Application;
import com.matchiq.application.domain.ApplicationStatus;
import com.matchiq.application.dto.ApplicationResponse;
import com.matchiq.application.dto.CreateApplicationRequest;
import com.matchiq.application.dto.UpdateApplicationRequest;
import com.matchiq.application.mapper.ApplicationMapper;
import com.matchiq.application.repository.ApplicationRepository;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.user.repository.UserRepository;
import com.matchiq.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository repository;
    private final VacancyRepository vacancyRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper mapper;

    @Transactional
    public ApplicationResponse create(Long userId, CreateApplicationRequest req) {
        vacancyRepository.findByIdAndUserId(req.getVacancyId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + req.getVacancyId()));
        if (req.getResumeId() != null) {
            resumeRepository.findByIdAndUserId(req.getResumeId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + req.getResumeId()));
        }
        Application app = new Application();
        app.setUserId(userId);
        app.setVacancyId(req.getVacancyId());
        app.setResumeId(req.getResumeId());
        app.setMatchId(req.getMatchId());
        app.setStatus(req.getStatus() != null ? req.getStatus() : ApplicationStatus.APPLIED);
        app.setAppliedAt(LocalDateTime.now());
        app.setInterviewAt(parseDate(req.getInterviewAt()));
        app.setNotes(req.getNotes());
        app.setDocumentsJson(mapper.toJson(req.getDocuments()));
        return mapper.toResponse(repository.save(app));
    }

    @Transactional
    public ApplicationResponse update(Long userId, Long id, UpdateApplicationRequest req) {
        Application app = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        if (req.getStatus() != null) app.setStatus(req.getStatus());
        if (req.getInterviewAt() != null) app.setInterviewAt(parseDate(req.getInterviewAt()));
        if (req.getNotes() != null) app.setNotes(req.getNotes());
        if (req.getDocuments() != null) app.setDocumentsJson(mapper.toJson(req.getDocuments()));
        return mapper.toResponse(repository.save(app));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> findByUserId(Long userId, ApplicationStatus status) {
        List<Application> list = repository.findByUserIdOrderByCreatedAtDesc(userId);
        if (status != null) {
            list = list.stream().filter(a -> a.getStatus() == status).toList();
        }
        return list.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse findByIdAndUserId(Long userId, Long id) {
        Application app = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        return mapper.toResponse(app);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse findByVacancy(Long userId, Long vacancyId) {
        return repository.findByUserIdAndVacancyId(userId, vacancyId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No application for vacancy " + vacancyId));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Application app = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        repository.delete(app);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        repository.deleteByUserId(userId);
    }

    private LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
