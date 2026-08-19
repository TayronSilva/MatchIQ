package com.matchiq.resume.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.resume.domain.ProcessingStatus;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.dto.ResumeResponse;
import com.matchiq.resume.dto.UpdateResumeRequest;
import com.matchiq.resume.mapper.ResumeMapper;
import com.matchiq.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository repository;

    @Mock
    private ResumeMapper mapper;

    @InjectMocks
    private ResumeService service;

    private Resume resume;
    private ResumeResponse response;

    @BeforeEach
    void setUp() {
        resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1L);
        resume.setFileName("curriculo.pdf");
        resume.setFileType("application/pdf");
        resume.setFileSize(2048L);
        resume.setLanguage("pt-BR");
        resume.setVersion(1);
        resume.setProcessingStatus(ProcessingStatus.PENDING);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());

        response = new ResumeResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setFileName("curriculo.pdf");
        response.setFileType("application/pdf");
        response.setFileSize(2048L);
        response.setLanguage("pt-BR");
        response.setVersion(1);
        response.setProcessingStatus(ProcessingStatus.PENDING);
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
    }

    @Test
    void upload_shouldSaveResume() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(repository.countByUserId(1L)).thenReturn(0L);
        when(repository.save(any(Resume.class))).thenReturn(resume);
        when(mapper.toResponse(resume)).thenReturn(response);

        ResumeResponse result = service.upload(1L, file, "pt-BR");

        assertNotNull(result);
        assertEquals("curriculo.pdf", result.getFileName());
        assertEquals(1, result.getVersion());
        verify(repository).save(any(Resume.class));
    }

    @Test
    void upload_shouldIncrementVersionForEachResume() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "curriculo2.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(repository.countByUserId(1L)).thenReturn(2L);

        Resume newResume = new Resume();
        newResume.setId(3L);
        newResume.setUserId(1L);
        newResume.setFileName("curriculo2.pdf");
        newResume.setFileType("application/pdf");
        newResume.setFileSize(3L);
        newResume.setLanguage("pt-BR");
        newResume.setVersion(3);

        ResumeResponse newResponse = new ResumeResponse();
        newResponse.setId(3L);
        newResponse.setUserId(1L);
        newResponse.setFileName("curriculo2.pdf");
        newResponse.setVersion(3);

        when(repository.save(any(Resume.class))).thenReturn(newResume);
        when(mapper.toResponse(newResume)).thenReturn(newResponse);

        ResumeResponse result = service.upload(1L, file, "pt-BR");

        assertEquals(3, result.getVersion());
    }

    @Test
    void upload_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vazio.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.upload(1L, file, "pt-BR"));
        verify(repository, never()).save(any());
    }

    @Test
    void upload_shouldRejectUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> service.upload(1L, file, "pt-BR"));
        verify(repository, never()).save(any());
    }

    @Test
    void findByUserId_shouldReturnList() {
        when(repository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(resume));
        when(mapper.toResponse(resume)).thenReturn(response);

        List<ResumeResponse> result = service.findByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("curriculo.pdf", result.get(0).getFileName());
    }

    @Test
    void findByIdAndUserId_shouldReturnResume() {
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume));
        when(mapper.toResponse(resume)).thenReturn(response);

        ResumeResponse result = service.findByIdAndUserId(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findByIdAndUserId_shouldThrowWhenNotFound() {
        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByIdAndUserId(99L, 1L));
    }

    @Test
    void updateLanguage_shouldUpdateLanguage() {
        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setLanguage("en");

        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume));
        when(repository.save(resume)).thenReturn(resume);

        ResumeResponse updated = new ResumeResponse();
        updated.setId(1L);
        updated.setLanguage("en");
        when(mapper.toResponse(resume)).thenReturn(updated);

        ResumeResponse result = service.updateLanguage(1L, 1L, request);

        assertEquals("en", result.getLanguage());
        verify(repository).save(resume);
    }

    @Test
    void delete_shouldDeleteResume() {
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume));

        service.delete(1L, 1L);

        verify(repository).delete(resume);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L, 1L));
        verify(repository, never()).delete(any());
    }
}
