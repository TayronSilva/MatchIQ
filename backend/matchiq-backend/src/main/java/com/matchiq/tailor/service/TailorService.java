package com.matchiq.tailor.service;

import com.matchiq.common.ai.AiClient;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.tailor.domain.ResumeSession;
import com.matchiq.tailor.domain.TailorStatus;
import com.matchiq.tailor.dto.CreateSessionRequest;
import com.matchiq.tailor.dto.TailorResponse;
import com.matchiq.tailor.mapper.TailorMapper;
import com.matchiq.tailor.repository.ResumeSessionRepository;
import com.matchiq.user.repository.UserRepository;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TailorService {

    private final ResumeSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final MatchMapper matchMapper;
    private final AiClient aiClient;
    private final TailorMapper tailorMapper;
    private final PdfGenerator pdfGenerator;

    private static final String SYSTEM =
            "Você é um redator de currículos especialista em ATS (Gupy, LinkedIn).";

    @Transactional
    public TailorResponse create(Long userId, CreateSessionRequest request) {
        resumeRepository.findByIdAndUserId(request.getResumeId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + request.getResumeId()));
        vacancyRepository.findByIdAndUserId(request.getVacancyId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + request.getVacancyId()));

        ResumeSession session = new ResumeSession();
        session.setUserId(userId);
        session.setResumeId(request.getResumeId());
        session.setVacancyId(request.getVacancyId());
        session.setStatus(TailorStatus.PENDING);
        ResumeSession saved = sessionRepository.save(session);

        generateAsync(userId, saved.getId());
        return tailorMapper.toResponse(saved);
    }

    @Async
    @Transactional
    public void generateAsync(Long userId, Long sessionId) {
        try {
            ResumeSession session = sessionRepository.findByIdAndUserId(sessionId, userId).orElse(null);
            if (session == null) {
                return;
            }
            Resume resume = resumeRepository.findByIdAndUserId(session.getResumeId(), userId).orElse(null);
            Vacancy vacancy = vacancyRepository.findByIdAndUserId(session.getVacancyId(), userId).orElse(null);
            if (resume == null || vacancy == null
                    || resume.getExtractedText() == null || resume.getExtractedText().isBlank()) {
                fail(session);
                return;
            }
            Match match = matchRepository.findByResumeIdAndVacancyId(session.getResumeId(), session.getVacancyId())
                    .orElse(null);
            String content = aiClient.chat(SYSTEM, buildPrompt(resume.getExtractedText(), vacancy, match));
            if (content == null || content.isBlank()) {
                fail(session);
                return;
            }
            session.setContentMarkdown(content);
            session.setStatus(TailorStatus.COMPLETED);
            sessionRepository.save(session);
        } catch (Exception e) {
            log.warn("Geração de CV tailor falhou para sessão {}: {}", sessionId, e.getMessage());
            sessionRepository.findByIdAndUserId(sessionId, userId).ifPresent(this::fail);
        }
    }

    @Transactional(readOnly = true)
    public List<TailorResponse> findByUserId(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(tailorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TailorResponse findByIdAndUserId(Long userId, Long id) {
        ResumeSession session = sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailor session not found with id: " + id));
        return tailorMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public byte[] renderPdf(Long userId, Long id) {
        ResumeSession session = sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailor session not found with id: " + id));
        if (session.getStatus() != TailorStatus.COMPLETED || session.getContentMarkdown() == null) {
            throw new IllegalStateException("O currículo ainda não foi gerado para esta sessão.");
        }
        return pdfGenerator.markdownToPdf(session.getContentMarkdown());
    }

    private void fail(ResumeSession session) {
        session.setStatus(TailorStatus.FAILED);
        sessionRepository.save(session);
    }

    private String buildPrompt(String resumeText, Vacancy vacancy, Match match) {
        String matched = match != null
                ? String.join(", ", matchMapper.readListForMatch(match.getMatchedSkillsJson()))
                : "";
        String missing = match != null
                ? String.join(", ", matchMapper.readListForMatch(match.getMissingSkillsJson()))
                : "";
        String company = (vacancy.getCompany() != null && !vacancy.getCompany().isBlank())
                ? " (" + vacancy.getCompany() + ")" : "";

        return String.format(
                "Currículo atual do candidato (texto extraído):\n---\n%s\n---\n\n" +
                "Descrição da vaga (%s%s):\n%s\n\n" +
                "Skills do candidato que combinam com a vaga: %s\n" +
                "Skills em falta: %s\n\n" +
                "Reescreva o currículo em MARKDOWN, otimizado para ATS (Gupy/LinkedIn), destacando as experiências " +
                "e skills que combinam com a vaga e reorganizando o conteúdo para maior aderência. " +
                "NÃO invente experiências, empresas, datas ou skills que o candidato não tenha no currículo atual.\n" +
                "Estruture como:\n## Resumo\n## Experiência (reorganizada)\n## Habilidades (destacando as que combinam)\n" +
                "## Sugestões de desenvolvimento (o que o candidato deveria adicionar para as skills em falta: %s)\n" +
                "Seja conciso e profissional, em português.",
                resumeText, vacancy.getTitle(), company, vacancy.getDescription(), matched, missing, missing);
    }
}
