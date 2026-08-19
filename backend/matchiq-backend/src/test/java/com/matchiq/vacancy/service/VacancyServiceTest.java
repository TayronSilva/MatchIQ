package com.matchiq.vacancy.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.profile.domain.WorkModality;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.skill.service.SkillExtractorService;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.vacancy.domain.VacancySource;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.mapper.VacancyMapper;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import com.matchiq.vacancy.service.VacancyScraper.ScrapedVacancy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository repository;

    @Mock
    private VacancySkillRepository vacancySkillRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillExtractorService skillExtractor;

    @Mock
    private VacancyMapper mapper;

    @Mock
    private VacancyScraper scraper;

    @InjectMocks
    private VacancyService service;

    private Vacancy vacancy;
    private VacancyResponse response;

    @BeforeEach
    void setUp() {
        vacancy = new Vacancy();
        vacancy.setId(1L);
        vacancy.setUserId(1L);
        vacancy.setTitle("Desenvolvedor Java");
        vacancy.setDescription("Vaga para Java com Spring Boot e PostgreSQL");
        vacancy.setCompany("MatchIQ Inc");
        vacancy.setLocation("Remoto");
        vacancy.setWorkModality(WorkModality.REMOTE);
        vacancy.setSource(VacancySource.MANUAL);
        vacancy.setCreatedAt(LocalDateTime.now());
        vacancy.setUpdatedAt(LocalDateTime.now());

        response = new VacancyResponse();
        response.setId(1L);
        response.setUserId(1L);
        response.setTitle("Desenvolvedor Java");
        response.setDescription("Vaga para Java com Spring Boot e PostgreSQL");
        response.setSource(VacancySource.MANUAL);
        response.setSkills(List.of());
        response.setCreatedAt(vacancy.getCreatedAt());
        response.setUpdatedAt(vacancy.getUpdatedAt());
    }

    @Test
    void create_shouldSaveVacancyAndLinkSkills() {
        CreateVacancyRequest request = new CreateVacancyRequest();
        request.setTitle("Desenvolvedor Java");
        request.setDescription("Vaga para Java com Spring Boot e PostgreSQL");

        Skill javaSkill = new Skill();
        javaSkill.setId(10L);
        javaSkill.setName("Java");

        when(mapper.toEntity(1L, request)).thenReturn(vacancy);
        when(repository.save(vacancy)).thenReturn(vacancy);
        when(skillExtractor.extract("Vaga para Java com Spring Boot e PostgreSQL")).thenReturn(List.of("Java", "Spring Boot"));
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));
        when(skillRepository.findByNameIgnoreCase("Spring Boot")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });
        when(vacancySkillRepository.existsByVacancyIdAndSkillId(1L, 10L)).thenReturn(false);
        when(vacancySkillRepository.existsByVacancyIdAndSkillId(1L, 11L)).thenReturn(false);
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of());
        when(mapper.toResponse(eq(vacancy), anyList())).thenReturn(response);

        VacancyResponse result = service.create(1L, request);

        assertNotNull(result);
        assertEquals("Desenvolvedor Java", result.getTitle());
        verify(vacancySkillRepository, times(2)).save(any(VacancySkill.class));
    }

    @Test
    void createFromUrl_shouldScrapeAndSave() {
        when(scraper.scrape("https://exemplo.com/vaga")).thenReturn(new ScrapedVacancy("Vaga Java", "Precisa de Java e Docker"));

        Skill javaSkill = new Skill();
        javaSkill.setId(10L);
        javaSkill.setName("Java");

        when(repository.save(any(Vacancy.class))).thenAnswer(inv -> {
            Vacancy v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });
        when(skillExtractor.extract("Precisa de Java e Docker")).thenReturn(List.of("Java", "Docker"));
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));
        when(skillRepository.findByNameIgnoreCase("Docker")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });
        when(vacancySkillRepository.existsByVacancyIdAndSkillId(1L, 10L)).thenReturn(false);
        when(vacancySkillRepository.existsByVacancyIdAndSkillId(1L, 11L)).thenReturn(false);
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of());
        when(mapper.toResponse(any(Vacancy.class), anyList())).thenReturn(response);

        VacancyResponse result = service.createFromUrl(1L, "https://exemplo.com/vaga");

        assertNotNull(result);
        verify(repository).save(any(Vacancy.class));
    }

    @Test
    void createFromUrl_shouldPropagateScrapeError() {
        when(scraper.scrape("https://exemplo.com/erro"))
                .thenThrow(new VacancyScrapeException("Não foi possível ler a vaga"));

        assertThrows(VacancyScrapeException.class, () -> service.createFromUrl(1L, "https://exemplo.com/erro"));
        verify(repository, never()).save(any());
    }

    @Test
    void findByUserId_shouldReturnList() {
        when(repository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(vacancy));
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of());
        when(mapper.toResponse(eq(vacancy), anyList())).thenReturn(response);

        List<VacancyResponse> result = service.findByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("Desenvolvedor Java", result.get(0).getTitle());
    }

    @Test
    void findByIdAndUserId_shouldReturnVacancy() {
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy));
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of());
        when(mapper.toResponse(eq(vacancy), anyList())).thenReturn(response);

        VacancyResponse result = service.findByIdAndUserId(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findByIdAndUserId_shouldThrowWhenNotFound() {
        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByIdAndUserId(99L, 1L));
    }

    @Test
    void update_shouldUpdateAndRelinkSkills() {
        UpdateVacancyRequest request = new UpdateVacancyRequest();
        request.setTitle("Desenvolvedor Java Sênior");
        request.setDescription("Vaga com Java e AWS");

        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy));
        doAnswer(inv -> {
            vacancy.setDescription(request.getDescription());
            return null;
        }).when(mapper).updateEntity(vacancy, request);
        when(repository.save(vacancy)).thenReturn(vacancy);
        when(skillExtractor.extract("Vaga com Java e AWS")).thenReturn(List.of("Java", "AWS"));
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(skillRepository.findByNameIgnoreCase("AWS")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });
        when(vacancySkillRepository.existsByVacancyIdAndSkillId(1L, 11L)).thenReturn(false);
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of());
        when(mapper.toResponse(eq(vacancy), anyList())).thenReturn(response);

        VacancyResponse result = service.update(1L, 1L, request);

        assertNotNull(result);
        verify(vacancySkillRepository).deleteByVacancyId(1L);
    }

    @Test
    void delete_shouldDeleteVacancy() {
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy));

        service.delete(1L, 1L);

        verify(repository).delete(vacancy);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(repository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L, 1L));
        verify(repository, never()).delete(any());
    }
}
