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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
            "Você é um redator de currículos especialista em ATS (Gupy, LinkedIn, RH tradicional). " +
            "Você REESCREVE o currículo de um candidato em Markdown, otimizado para sistemas de triagem, " +
            "deixando-o mais atraente e aderente à vaga (sem inventar nada). " +
            "Regras absolutas: (1) NUNCA invente empresas, cargos, datas, skills, números ou métricas " +
            "que não estejam no currículo original. (2) Você produz SOMENTE o currículo em Markdown. " +
            "NÃO inclua explicações, comentários, planejamento, nem texto do tipo 'Vou verificar' ou " +
            "'O usuário quer'. Comece o documento diretamente com o NOME COMPLETO do candidato como " +
            "título H1, sem nenhum texto introdutório. " +
            "Regra de completude: gere o documento INTEIRO, na ordem (Dados de contato, Objetivo, " +
            "Resumo Profissional, Formação Acadêmica, Experiência Profissional, Atividades Complementares, " +
            "Competências Técnicas). NÃO interrompa o texto e NÃO pare antes de fechar a última seção. " +
            "Inclua TODAS as experiências e o voluntariado que existirem no currículo original.";

    private static final String GENERAL_GUIDELINES = loadResource("tailor/general-guidelines.txt");
    private static final String GUPY_GUIDELINES = loadResource("tailor/gupy-guidelines.txt");

    private static String loadResource(String path) {
        try (InputStream in = TailorService.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return "";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static boolean isGupy(Vacancy vacancy) {
        String url = vacancy.getUrl();
        String hay = ((vacancy.getTitle() != null ? vacancy.getTitle() : "") + " "
                + (vacancy.getDescription() != null ? vacancy.getDescription() : "") + " "
                + (url != null ? url : "")).toLowerCase();
        return hay.contains("gupy");
    }

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

            String content = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                String candidate = aiClient.chat(SYSTEM, buildPrompt(resume.getExtractedText(), vacancy, match));
                if (candidate != null && !candidate.isBlank() && isComplete(candidate)) {
                    content = candidate;
                    break;
                }
                if (candidate != null && !candidate.isBlank()) {
                    log.warn("Tentativa {} da sessão {} gerou CV incompleto (truncado); reenviando ao modelo...",
                            attempt, sessionId);
                }
                content = candidate;
            }
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

    @Transactional
    public void delete(Long userId, Long id) {
        ResumeSession session = sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailor session not found with id: " + id));
        sessionRepository.delete(session);
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

    /**
     * Verifica se o Markdown gerado não foi truncado pelo modelo: as seções críticas
     * (que costumam ficar de fora quando a IA para cedo) precisam estar presentes.
     */
    private boolean isComplete(String content) {
        if (content == null) {
            return false;
        }
        return content.contains("## Experiência Profissional")
                && content.contains("## Atividades Complementares")
                && content.contains("## Competências Técnicas");
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
        boolean gupy = isGupy(vacancy);

        StringBuilder sb = new StringBuilder();
        sb.append("Currículo atual do candidato (texto extraído):\n---\n").append(resumeText).append("\n---\n\n");
        sb.append("Descrição da vaga (").append(vacancy.getTitle()).append(company).append("):\n")
                .append(vacancy.getDescription()).append("\n\n");
        sb.append("Skills do candidato que combinam com a vaga: ").append(matched).append("\n");
        sb.append("Skills em falta: ").append(missing).append("\n\n");
        sb.append(GENERAL_GUIDELINES).append("\n\n");
        if (gupy) {
            sb.append(GUPY_GUIDELINES).append("\n\n");
        }
        sb.append("REESCREVA o currículo em MARKDOWN, otimizado para ATS");
        if (gupy) sb.append(" (especialmente a plataforma Gupy)");
        sb.append(", seguindo as seções e regras acima, destacando as experiências e skills que combinam com a vaga ")
                .append("e reorganizando o conteúdo para maior aderência. ");
        sb.append("NÃO invente experiências, empresas, datas, skills, números ou métricas que o candidato não tenha no currículo atual. ")
                .append("Se o currículo original não tem dados quantitativos, não os invente.\n");
        sb.append("Estruture como:\n## Dados de contato\n## Objetivo\n## Resumo Profissional\n")
                .append("## Formação Acadêmica\n## Experiência Profissional\n## Atividades Complementares\n")
                .append("## Competências Técnicas");
        sb.append("\nData atual: ").append(LocalDate.now()).append(". ")
                .append("Use-a para deduzir corretamente se uma formação ou experiência está concluída ou em andamento, ")
                .append("com base nas datas informadas (não invente o status se o original já traz a data de conclusão).");
        if (gupy) {
            sb.append("\nAplique o método ERAF em cada experiência, usando números e métricas APENAS se já existirem no currículo original.");
        }
        sb.append("\nIMPORTANTE — FORMATO DA RESPOSTA:\n");
        sb.append("- Retorne APENAS o currículo em Markdown. Nada antes do H1 com o nome, nada depois.\n");
        sb.append("- Proibido: frases como 'Vou verificar', 'O usuário quer', 'A seguir', explicações ou qualquer metarrelato.\n");
        sb.append("- Nos Dados de contato, preserve exatamente nome, telefone, e-mail, LinkedIn e GitHub/portfólio do original. ");
        sb.append("Se os rótulos estiverem trocados (ex.: o link do LinkedIn aparece sob 'GitHub' e vice-versa), CORRIJA o rótulo usando a própria URL como referência — não invente novas URLs.\n");
        sb.append("Seja conciso e profissional, em português.");
        return sb.toString();
    }
}
