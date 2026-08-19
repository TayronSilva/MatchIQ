package com.matchiq.resume.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.resume.domain.ProcessingStatus;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.dto.ResumeResponse;
import com.matchiq.resume.dto.UpdateResumeRequest;
import com.matchiq.resume.mapper.ResumeMapper;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.repository.ResumeSkillRepository;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.skill.service.SkillExtractorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final List<String> ALLOWED_TYPES = List.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private final ResumeRepository repository;
    private final ResumeMapper mapper;
    private final ResumeTextExtractor textExtractor;
    private final SkillExtractorService skillExtractor;
    private final SkillRepository skillRepository;
    private final ResumeSkillRepository resumeSkillRepository;

    @Transactional
    public ResumeResponse upload(Long userId, MultipartFile file, String language) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF and DOCX files are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must be at most 10 MB");
        }

        long version = repository.countByUserId(userId) + 1;

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileType(file.getContentType());
        resume.setFileSize(file.getSize());
        resume.setLanguage(language);
        resume.setVersion((int) version);

        String extractedText = null;
        try {
            extractedText = textExtractor.extract(file);
            resume.setExtractedText(extractedText);
            resume.setProcessingStatus(ProcessingStatus.COMPLETED);
        } catch (IOException | RuntimeException e) {
            resume.setProcessingStatus(ProcessingStatus.FAILED);
        }

        Resume saved = repository.save(resume);

        if (extractedText != null && !extractedText.isBlank()) {
            linkExtractedSkills(saved.getId(), skillExtractor.extract(extractedText));
        }

        return mapper.toResponse(saved);
    }

    private void linkExtractedSkills(Long resumeId, List<String> skillNames) {
        for (String name : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> skillRepository.save(createSkill(name)));

            if (!resumeSkillRepository.existsByResumeIdAndSkillId(resumeId, skill.getId())) {
                ResumeSkill resumeSkill = new ResumeSkill();
                resumeSkill.setResumeId(resumeId);
                resumeSkill.setSkillId(skill.getId());
                resumeSkillRepository.save(resumeSkill);
            }
        }
    }

    private Skill createSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skill;
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> findByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse findByIdAndUserId(Long id, Long userId) {
        Resume resume = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + id));
        return mapper.toResponse(resume);
    }

    @Transactional
    public ResumeResponse updateLanguage(Long id, Long userId, UpdateResumeRequest request) {
        Resume resume = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + id));

        resume.setLanguage(request.getLanguage());
        Resume updated = repository.save(resume);
        return mapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Resume resume = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + id));
        repository.delete(resume);
    }
}
