package com.matchiq;

import com.matchiq.analysis.service.AnalysisService;
import com.matchiq.match.service.MatchService;
import com.matchiq.profile.dto.CreateProfileRequest;
import com.matchiq.profile.service.ProfileService;
import com.matchiq.recommendation.service.RecommendationService;
import com.matchiq.resume.dto.ResumeResponse;
import com.matchiq.resume.service.ResumeService;
import com.matchiq.skill.dto.CreateSkillRequest;
import com.matchiq.skill.service.SkillService;
import com.matchiq.user.dto.CreateUserRequest;
import com.matchiq.user.dto.UserResponse;
import com.matchiq.user.service.UserService;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.service.VacancyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração: valida o fluxo completo do MatchIQ com o contexto Spring real
 * (wiring de beans, repositórios, services), do cadastro à recomendação.
 */
@SpringBootTest
@ActiveProfiles("test")
class MatchIQFlowIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private RecommendationService recommendationService;

    @Test
    void fullFlow_shouldWorkEndToEnd() {
        // 1. Cria usuário
        CreateUserRequest createUser = new CreateUserRequest();
        createUser.setName("João Teste");
        createUser.setEmail("joao.flow@email.com");
        createUser.setPassword("senha12345");
        UserResponse user = userService.create(createUser);
        assertNotNull(user.getId());

        // 2. Cria perfil
        CreateProfileRequest createProfile = new CreateProfileRequest();
        createProfile.setHeadline("Java Backend Developer");
        createProfile.setBio("Desenvolvedor backend");
        var profile = profileService.create(user.getId(), createProfile);
        assertNotNull(profile.getId());

        // 3. Upload de currículo (PDF simulado)
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "curriculo.pdf", "application/pdf",
                "Java Spring Boot PostgreSQL Docker".getBytes());
        ResumeResponse resume = resumeService.upload(user.getId(), pdf, "pt-BR");
        assertNotNull(resume.getId());
        assertEquals("curriculo.pdf", resume.getFileName());

        // 4. Cria skills e vincula ao currículo
        var javaSkill = skillService.create(skillRequest("Java"));
        var springSkill = skillService.create(skillRequest("Spring Boot"));
        assertNotNull(javaSkill.getId());
        assertNotNull(springSkill.getId());

        // 5. Cria vaga manual
        CreateVacancyRequest createVacancy = new CreateVacancyRequest();
        createVacancy.setTitle("Desenvolvedor Java");
        createVacancy.setDescription("Vaga para Java com Spring Boot e AWS");
        var vacancy = vacancyService.create(user.getId(), createVacancy);
        assertNotNull(vacancy.getId());

        // 6. Calcula o match (Java e Spring Boot o candidato tem; AWS falta)
        var match = matchService.calculate(user.getId(), resume.getId(), vacancy.getId());
        assertNotNull(match.getScore());
        assertTrue(match.getScore() >= 0 && match.getScore() <= 100);

        // 7. Gera análise
        var analysis = analysisService.generate(user.getId(), match.getId());
        assertNotNull(analysis.getObservations());

        // 8. Gera recomendação (pode cair no fallback local se a IA falhar)
        var recommendation = recommendationService.generate(user.getId(), match.getId());
        assertNotNull(recommendation.getStudyPlan());
        assertFalse(recommendation.getStudyPlan().isBlank());
    }

    private CreateSkillRequest skillRequest(String name) {
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName(name);
        return request;
    }
}
