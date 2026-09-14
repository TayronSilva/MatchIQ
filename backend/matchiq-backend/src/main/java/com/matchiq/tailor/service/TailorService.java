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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    private static final String RESUME_TEMPLATE = loadResource("knowledge/resume-template.txt");
    private static final String GUPY_GUIDE = loadResource("knowledge/gupy-guide.txt");
    private static final String GENERAL_GUIDELINES = loadResource("tailor/general-guidelines.txt");
    private static final String GUPY_GUIDELINES = loadResource("tailor/gupy-guidelines.txt");

    private static final Set<String> REQUIRED_SECTIONS = Set.of(
            "objetivo", "resumo", "formação", "formacao", "experiência", "experiencia", "competências", "competencias"
    );

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
            if (session == null) return;

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
                try {
                    String systemPrompt = buildSystemPrompt(isGupy(vacancy));
                    String userPrompt = buildUserPrompt(resume.getExtractedText(), vacancy, match);
                    String candidate = aiClient.chat(systemPrompt, userPrompt);
                    if (candidate != null && !candidate.isBlank() && isComplete(candidate)) {
                        content = candidate;
                        break;
                    }
                    if (candidate != null && !candidate.isBlank()) {
                        log.warn("Tentativa {} da sessão {} gerou CV incompleto; reenviando...", attempt, sessionId);
                        content = candidate;
                    }
                } catch (Exception e) {
                    log.warn("Tentativa {} da sessão {} falhou na IA: {}", attempt, sessionId, e.getMessage());
                }
            }

            if (content == null || content.isBlank()) {
                log.info("IA indisponível para sessão {}; gerando CV fallback", sessionId);
                content = buildFallbackCv(resume.getExtractedText(), vacancy, match);
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

    private String buildSystemPrompt(boolean gupy) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é um especialista em currículos para ATS (Gupy, LinkedIn, RH tradicional).\n");
        sb.append("Sua tarefa: REESCREVER o currículo do candidato em Markdown, estruturado e pronto para passar por sistemas de ATS.\n\n");

        sb.append("--- MODELO DE REFERÊNCIA (siga esta estrutura e ordem de seções) ---\n\n");
        sb.append(RESUME_TEMPLATE);
        sb.append("\n\n--- FIM DO MODELO ---\n\n");

        sb.append(GENERAL_GUIDELINES);
        sb.append("\n\n");

        if (gupy) {
            sb.append("--- REGRAS ESPECÍFICAS PARA GUPY ---\n\n");
            sb.append(GUPY_GUIDELINES);
            sb.append("\n\n");
            sb.append("--- CONTEXTO AVANÇADO SOBRE A GUPY 2026 ---\n\n");
            sb.append(GUPY_GUIDE);
            sb.append("\n\n");
        }

        sb.append("REGRAS ABSOLUTAS (não negociáveis):\n");
        sb.append("1. NUNCA invente empresas, cargos, datas, skills, números ou métricas ausentes no currículo original.\n");
        sb.append("2. Preserve TODOS os dados de contato do original (nome, telefone, e-mail, LinkedIn, GitHub).\n");
        sb.append("3. Cada bullet point: verbo de ação + o que fez + ferramenta + resultado (método ERAF quando aplicável).\n");
        sb.append("4. Gere o documento COMPLETO com todas as seções do modelo, sem interromper.\n");
        sb.append("5. Comece APENAS com # NOME. Sem texto introdutório, sem explicações.\n");
        sb.append("6. Em português, sem erros de ortografia.\n");
        sb.append("7. Não inclua comentários, metarrelato ou texto fora do currículo.\n");
        return sb.toString();
    }

    private String buildUserPrompt(String resumeText, Vacancy vacancy, Match match) {
        String matched = match != null
                ? String.join(", ", matchMapper.readListForMatch(match.getMatchedSkillsJson()))
                : "";
        String missing = match != null
                ? String.join(", ", matchMapper.readListForMatch(match.getMissingSkillsJson()))
                : "";
        String company = (vacancy.getCompany() != null && !vacancy.getCompany().isBlank())
                ? " (" + vacancy.getCompany() + ")" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("=== CURRÍCULO ATUAL DO CANDIDATO (texto extraído) ===\n");
        sb.append(resumeText).append("\n\n");

        sb.append("=== VAGA ALVO ===\n");
        sb.append("Título: ").append(vacancy.getTitle()).append(company).append("\n");
        sb.append("Descrição:\n").append(vacancy.getDescription()).append("\n\n");

        if (!matched.isEmpty() || !missing.isEmpty()) {
            sb.append("=== ANÁLISE DE COMPATIBILIDADE ===\n");
            sb.append("Skills que combinam: ").append(matched).append("\n");
            sb.append("Skills em falta: ").append(missing).append("\n\n");
        }

        sb.append("Data atual: ").append(LocalDate.now()).append(". ");
        sb.append("Use-a para deduzir se formação/experiência está concluída ou em andamento.\n\n");

        sb.append("Reescreva o currículo acima seguindo o modelo e as diretrizes. ");
        sb.append("Mantenha honestidade total com os dados do candidato.");
        return sb.toString();
    }

    private boolean isComplete(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        long sectionCount = REQUIRED_SECTIONS.stream()
                .filter(s -> lower.contains("## " + s) || lower.contains("## " + s))
                .count();
        return sectionCount >= 4 && content.length() > 300;
    }

    private String buildFallbackCv(String resumeText, Vacancy vacancy, Match match) {
        String[] lines = resumeText.split("\\n");

        String name = "CANDIDATO";
        for (String line : lines) {
            String l = line.trim();
            if (!l.isEmpty() && l.length() < 60 && !l.contains("@") && !l.contains("(")
                    && !l.toLowerCase().contains("curriculo") && !l.toLowerCase().contains("currículo")
                    && !l.toLowerCase().contains("cv") && !l.toLowerCase().contains("tel")) {
                name = l;
                break;
            }
        }

        String currentSection = "";
        StringBuilder sectionContent = new StringBuilder();
        java.util.Map<String, String> sections = new java.util.LinkedHashMap<>();

        for (String line : lines) {
            String l = line.trim();
            if (l.isEmpty()) continue;

            String lower = l.toLowerCase()
                    .replaceAll("[áàãâ]", "a").replaceAll("[éèê]", "e")
                    .replaceAll("[íìî]", "i").replaceAll("[óòõô]", "o")
                    .replaceAll("[úùû]", "u").replaceAll("[ç]", "c");

            boolean isHeader = l.length() < 80;
            boolean isExperienceHeader = isHeader && (
                    lower.startsWith("experiencia") && lower.contains("profissional")
                    || lower.startsWith("experiência") && lower.contains("profissional")
                    || lower.equals("professional experience") || lower.equals("work experience"));
            boolean isEducationHeader = isHeader && (
                    lower.startsWith("formacao") || lower.startsWith("formação")
                    || lower.equals("educacao") || lower.equals("educação")
                    || lower.equals("education") || lower.equals("academic"));
            boolean isSkillsHeader = isHeader && (
                    lower.startsWith("competencias") || lower.startsWith("competências")
                    || lower.equals("skills") || lower.equals("technical skills")
                    || lower.equals("habilidades"));
            boolean isActivitiesHeader = isHeader && (
                    lower.startsWith("atividades")
                    || lower.equals("voluntariado") || lower.equals("projetos")
                    || lower.startsWith("licencas") || lower.startsWith("licenças")
                    || lower.startsWith("certificados"));
            boolean isSummaryHeader = isHeader && (
                    lower.equals("resumo profissional") || lower.equals("resumo")
                    || lower.equals("summary") || lower.equals("perfil profissional"));
            boolean isContactHeader = isHeader && (
                    lower.startsWith("dados de contato") || lower.equals("contato")
                    || lower.equals("contact") || lower.equals("contact info")
                    || lower.equals("informações de contato") || lower.equals("informacoes de contato"));
            boolean isObjectiveHeader = isHeader && (
                    lower.equals("objetivo") || lower.equals("objective"));

            if (isExperienceHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "experiencia";
                sectionContent = new StringBuilder();
            } else if (isEducationHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "formacao";
                sectionContent = new StringBuilder();
            } else if (isSkillsHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "competencias";
                sectionContent = new StringBuilder();
            } else if (isActivitiesHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "atividades";
                sectionContent = new StringBuilder();
            } else if (isSummaryHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "resumo";
                sectionContent = new StringBuilder();
            } else if (isContactHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "contato";
                sectionContent = new StringBuilder();
            } else if (isObjectiveHeader) {
                if (!currentSection.isEmpty() && sectionContent.length() > 0)
                    sections.put(currentSection, sectionContent.toString().trim());
                currentSection = "objetivo";
                sectionContent = new StringBuilder();
            } else if (currentSection.isEmpty()) {
                currentSection = "contato";
                sectionContent = new StringBuilder();
            }

            if (!currentSection.isEmpty()) {
                sectionContent.append(line).append("\n");
            }
        }
        if (!currentSection.isEmpty() && sectionContent.length() > 0)
            sections.put(currentSection, sectionContent.toString().trim());

        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(name).append("\n");

        if (sections.containsKey("contato")) {
            String contato = sections.get("contato");
            for (String cl : contato.split("\\n")) {
                String clTrim = cl.trim();
                if (!clTrim.isEmpty() && !clTrim.equalsIgnoreCase(name)) {
                    sb.append(clTrim).append("\n");
                }
            }
        }
        sb.append("\n");

        sb.append("## OBJETIVO\n");
        if (sections.containsKey("objetivo")) {
            String obj = sections.get("objetivo")
                    .replaceAll("(?i)^objetivo\\s*[:\\-]?\\s*", "").trim();
            sb.append(obj).append("\n\n");
        } else {
            sb.append(vacancy.getTitle() != null ? vacancy.getTitle() : "Desenvolvedor(a)").append("\n\n");
        }

        sb.append("## RESUMO PROFISSIONAL\n");
        if (sections.containsKey("resumo")) {
            String resumo = sections.get("resumo");
            resumo = stripSectionHeader(resumo, "resumo profissional", "resumo",
                    "summary", "perfil profissional", "perfil", "objective", "objetivo");
            resumo = cleanSectionContent(resumo);
            sb.append(resumo).append("\n\n");
        } else {
            sb.append(gerarResumoPersonalizado(resumeText, vacancy)).append("\n\n");
        }

        if (sections.containsKey("formacao")) {
            sb.append("## FORMAÇÃO ACADÊMICA\n\n");
            String formacao = sections.get("formacao");
            formacao = stripSectionHeader(formacao, "formação", "formacao", "educação", "educacao", "education", "academic");
            formacao = stripSectionHeader(formacao, "licenças", "licencas", "certificados");
            sb.append(cleanSectionContent(formacao)).append("\n\n");
        }

        if (sections.containsKey("experiencia")) {
            sb.append("## EXPERIÊNCIA PROFISSIONAL\n\n");
            String exp = sections.get("experiencia");
            exp = stripSectionHeader(exp, "experiência profissional", "experiencia profissional",
                    "professional experience", "work experience");
            sb.append(formatExperienceEntries(exp)).append("\n\n");
        }

        if (sections.containsKey("atividades")) {
            sb.append("## ATIVIDADES COMPLEMENTARES\n\n");
            String ativ = sections.get("atividades");
            ativ = stripSectionHeader(ativ, "atividades complementares", "atividades",
                    "voluntariado", "projetos");
            sb.append(formatExperienceEntries(ativ)).append("\n\n");
        }

        if (sections.containsKey("competencias")) {
            sb.append("## COMPETÊNCIAS TÉCNICAS\n\n");
            String comp = sections.get("competencias");
            comp = stripSectionHeader(comp, "competências técnicas", "competencias tecnicas",
                    "skills", "technical skills", "habilidades");
            sb.append(formatSkills(comp)).append("\n\n");
        }

        return sb.toString();
    }

    private String stripSectionHeader(String text, String... headers) {
        String result = text;
        for (String h : headers) {
            result = result.replaceAll("(?i)^" + java.util.regex.Pattern.quote(h) + "\\s*[:\\-]?\\s*", "").trim();
        }
        return result;
    }

    private String cleanSectionContent(String text) {
        if (text == null || text.isBlank()) return "";
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String l = line.trim();
            if (l.isEmpty()) continue;
            String lower = l.toLowerCase()
                    .replaceAll("[áàãâ]", "a").replaceAll("[éèê]", "e")
                    .replaceAll("[íìî]", "i").replaceAll("[óòõô]", "o")
                    .replaceAll("[úùû]", "u").replaceAll("[ç]", "c");
            boolean isKnownHeader = lower.startsWith("experiencia") || lower.startsWith("experiência")
                    || lower.startsWith("formacao") || lower.startsWith("formação")
                    || lower.startsWith("competencias") || lower.startsWith("competências")
                    || lower.startsWith("atividades") || lower.startsWith("licencas")
                    || lower.startsWith("licenças") || lower.startsWith("certificados")
                    || lower.equals("skills") || lower.equals("technical skills")
                    || lower.equals("habilidades") || lower.startsWith("resumo")
                    || lower.startsWith("perfil profissional") || lower.equals("perfil")
                    || lower.equals("summary") || lower.equals("objective")
                    || lower.startsWith("objetivo");
            if (isKnownHeader && l.length() < 60) continue;
            sb.append(l).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatExperienceEntries(String text) {
        if (text == null || text.isBlank()) return "";

        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        boolean firstEntry = true;

        for (String line : lines) {
            String l = line.trim();
            if (l.isEmpty()) continue;

            String lower = l.toLowerCase()
                    .replaceAll("[áàãâ]", "a").replaceAll("[éèê]", "e")
                    .replaceAll("[íìî]", "i").replaceAll("[óòõô]", "o")
                    .replaceAll("[úùû]", "u").replaceAll("[ç]", "c");

            boolean isKnownHeader = lower.startsWith("experiencia") || lower.startsWith("experiência")
                    || lower.startsWith("formacao") || lower.startsWith("formação")
                    || lower.startsWith("competencias") || lower.startsWith("competências")
                    || lower.startsWith("atividades") || lower.startsWith("licencas")
                    || lower.startsWith("licenças") || lower.startsWith("certificados")
                    || lower.equals("skills") || lower.equals("technical skills")
                    || lower.equals("habilidades") || lower.startsWith("resumo")
                    || lower.startsWith("objetivo");
            if (isKnownHeader && l.length() < 60) continue;

            boolean looksLikeCompany = isCompanyLine(l);

            if (looksLikeCompany) {
                if (!firstEntry) sb.append("\n");
                sb.append("**").append(l).append("**\n");
                firstEntry = false;
            } else {
                sb.append(l).append("\n");
            }
        }

        return sb.toString();
    }

    private boolean isCompanyLine(String l) {
        if (l.startsWith("*") || l.startsWith("-") || l.startsWith("•")) return false;
        if (l.startsWith("**")) return false;
        if (l.length() > 120) return false;

        String lower = l.toLowerCase()
                .replaceAll("[áàãâ]", "a").replaceAll("[éèê]", "e")
                .replaceAll("[íìî]", "i").replaceAll("[óòõô]", "o")
                .replaceAll("[úùû]", "u").replaceAll("[ç]", "c");

        boolean hasPipe = l.contains("|");
        boolean hasYear = l.matches(".*\\d{4}.*");
        boolean hasPeriod = lower.contains(" - ") || lower.contains("-presente") || lower.contains("-atual")
                || lower.matches(".*\\(.*\\d{4}.*\\).*");

        if (hasPipe && hasYear) return true;
        if (hasPipe && hasPeriod) return true;
        if (hasPeriod && l.length() < 80) {
            boolean startsWithVerb = lower.startsWith("desenvolvi") || lower.startsWith("atuei")
                    || lower.startsWith("implementei") || lower.startsWith("realizei")
                    || lower.startsWith("responsável") || lower.startsWith("responsavel")
                    || lower.startsWith("aprendi") || lower.startsWith("consigo")
                    || lower.startsWith("prestei") || lower.startsWith("criei")
                    || lower.startsWith("geri") || lower.startsWith("liderei")
                    || lower.startsWith("mentorei") || lower.startsWith("modelei")
                    || lower.startsWith("refin") || lower.startsWith("otimizei")
                    || lower.startsWith("organizei") || lower.startsWith("coorden")
                    || lower.startsWith("gestão") || lower.startsWith("gestao")
                    || lower.startsWith("prospecção") || lower.startsWith("prospeccao")
                    || lower.startsWith("análise") || lower.startsWith("analise")
                    || lower.startsWith("estruturação") || lower.startsWith("estruturacao")
                    || lower.startsWith("integração") || lower.startsWith("integracao")
                    || lower.startsWith("comunicação") || lower.startsWith("comunicacao")
                    || lower.startsWith("suporte") || lower.startsWith("fundação")
                    || lower.startsWith("fundacao");
            return !startsWithVerb;
        }

        return false;
    }

    private String formatSkills(String text) {
        if (text == null || text.isBlank()) return "";
        text = text.replaceAll("(?i)^competências\\s*(técnicas)?\\s*[:\\-]?\\s*", "")
                   .replaceAll("(?i)^skills?\\s*[:\\-]?\\s*", "").trim();
        if (text.length() < 5) return "";
        if (text.matches("(?i)^[a-záàãâéèêíìîóòõúùûç]+\\s*[:\\-].*")) {
            return text + "\n";
        }

        StringBuilder sb = new StringBuilder();
        List<String> langs = extractKeywords(text, new String[]{
            "Java", "Python", "JavaScript", "TypeScript", "C#", "C++", "PHP", "Ruby",
            "Go", "Rust", "Kotlin", "Swift", "Scala", "R", "MATLAB", "SQL"
        });
        if (!langs.isEmpty()) sb.append("Linguagens: ").append(String.join(", ", langs)).append(".\n");

        List<String> tools = extractKeywords(text, new String[]{
            "Spring", "Spring Boot", "Docker", "Kubernetes", "Git", "GitHub", "GitLab",
            "Jenkins", "CI/CD", "Maven", "Gradle", "React", "Angular", "Vue",
            "Node", "Express", "Django", "Flask", "FastAPI", "ASP.NET", "Laravel"
        });
        if (!tools.isEmpty()) sb.append("Ferramentas: ").append(String.join(", ", tools)).append(".\n");

        List<String> dbs = extractKeywords(text, new String[]{
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Oracle", "SQL Server",
            "Elasticsearch", "DynamoDB", "Cassandra", "SQLite"
        });
        if (!dbs.isEmpty()) sb.append("Banco de Dados: ").append(String.join(", ", dbs)).append(".\n");

        List<String> cloud = extractKeywords(text, new String[]{
            "AWS", "Azure", "GCP", "Google Cloud", "Heroku", "Vercel", "Linux", "Windows"
        });
        if (!cloud.isEmpty()) sb.append("Cloud/Infra: ").append(String.join(", ", cloud)).append(".\n");

        List<String> methods = extractKeywords(text, new String[]{
            "Scrum", "Kanban", "Agile", "Lean", "TDD", "BDD", "DDD"
        });
        if (!methods.isEmpty()) sb.append("Metodologias: ").append(String.join(", ", methods)).append(".\n");

        if (sb.isEmpty()) {
            String cleaned = text.replaceAll("\\s+", " ").trim();
            if (cleaned.length() > 100) cleaned = cleaned.substring(0, 100) + "...";
            return "* " + cleaned + "\n";
        }
        return sb.toString();
    }

    private List<String> extractKeywords(String text, String[] keywords) {
        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) found.add(kw);
        }
        return found;
    }

    private String gerarResumoPersonalizado(String resumeText, Vacancy vacancy) {
        String lower = resumeText.toLowerCase();

        String tempoExp = "mais de 3 anos";
        if (lower.contains("estagiário") || lower.contains("estagio")) {
            tempoExp = "experiência inicial";
        } else if (lower.contains("junior") || lower.contains("júnior")) {
            tempoExp = "mais de 2 anos";
        } else if (lower.contains("pleno") || lower.contains("mid-level")) {
            tempoExp = "mais de 4 anos";
        } else if (lower.contains("senior") || lower.contains("sênior")) {
            tempoExp = "mais de 6 anos";
        }

        List<String> techs = new ArrayList<>();
        String[] techKeywords = {"Java", "Spring", "Python", "JavaScript", "TypeScript",
                "React", "Angular", "Node", "Docker", "Kubernetes", "AWS", "Azure",
                "PostgreSQL", "MySQL", "MongoDB", "Redis", "Git", "CI/CD",
                "REST", "API", "JWT", "Microservices", "Linux", "SQL"};
        for (String tech : techKeywords) {
            if (lower.contains(tech.toLowerCase())) techs.add(tech);
        }
        String techStr = techs.isEmpty() ? "tecnologias modernas" :
                String.join(", ", techs.subList(0, Math.min(6, techs.size())));

        boolean hasLeadership = lower.contains("líder") || lower.contains("lider")
                || lower.contains("lead") || lower.contains("gestão") || lower.contains("gestao")
                || lower.contains("coordenador") || lower.contains("gerente")
                || lower.contains("scrum master") || lower.contains("product owner");

        StringBuilder sb = new StringBuilder();
        sb.append("Profissional de tecnologia com ").append(tempoExp);
        if (hasLeadership) sb.append(" e experiência em liderança técnica de equipes");
        sb.append(". ");
        if (!techs.isEmpty()) sb.append("Domínio em ").append(techStr).append(". ");
        sb.append("Focado em entregar soluções escaláveis e eficientes, ")
          .append("com atenção a boas práticas e qualidade de código.");
        return sb.toString();
    }
}
