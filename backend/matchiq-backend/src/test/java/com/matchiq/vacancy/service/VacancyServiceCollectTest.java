package com.matchiq.vacancy.service;

import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.skill.service.SkillExtractorService;
import com.matchiq.vacancy.collector.RawVacancy;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySource;
import com.matchiq.vacancy.mapper.VacancyMapper;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import com.matchiq.vacancy.service.ScrapedVacancy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VacancyServiceCollectTest {

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
    private com.matchiq.vacancy.service.VacancyScraper scraper;

    @InjectMocks
    private VacancyService service;

    @BeforeEach
    void setUp() {
        when(repository.save(any(Vacancy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillExtractor.extract(any())).thenReturn(List.of());
        when(vacancySkillRepository.findByVacancyId(anyLong())).thenReturn(List.of());
    }

    @Test
    void ingestCollected_shouldCreateWhenNew() {
        RawVacancy raw = new RawVacancy("ext-1", "Java Dev", "Acme", "Java e Spring",
                "https://x.com/1", "Remote", null, null, null);
        when(repository.findByUserIdAndSourceAndExternalId(1L, VacancySource.REMOTIVE, "ext-1"))
                .thenReturn(Optional.empty());

        IngestionOutcome outcome = service.ingestCollected(1L, VacancySource.REMOTIVE, raw);

        assertEquals(IngestionOutcome.CREATED, outcome);
        verify(repository).save(any(Vacancy.class));
    }

    @Test
    void ingestCollected_shouldUpdateWhenExisting() {
        Vacancy existing = new Vacancy();
        existing.setId(5L);
        existing.setUserId(1L);
        existing.setTitle("Old title");
        existing.setDescription("Old description");
        existing.setSource(VacancySource.REMOTIVE);
        existing.setExternalId("ext-1");

        RawVacancy raw = new RawVacancy("ext-1", "Java Dev", "Acme", "Java e Spring",
                "https://x.com/1", "Remote", null, null, null);
        when(repository.findByUserIdAndSourceAndExternalId(1L, VacancySource.REMOTIVE, "ext-1"))
                .thenReturn(Optional.of(existing));

        IngestionOutcome outcome = service.ingestCollected(1L, VacancySource.REMOTIVE, raw);

        assertEquals(IngestionOutcome.UPDATED, outcome);
        assertEquals("Java Dev", existing.getTitle());
        assertEquals("Java e Spring", existing.getDescription());
        assertFalse(existing.isRemoved());
        assertNotNull(existing.getLastSeenAt());
        verify(repository).save(existing);
    }

    @Test
    void ingestCollected_shouldUseUrlAsExternalIdWhenBlank() {
        RawVacancy raw = new RawVacancy(null, "Java Dev", "Acme", "Java",
                "https://x.com/1", null, null, null, null);
        when(repository.findByUserIdAndSourceAndExternalId(1L, VacancySource.REMOTIVE, "https://x.com/1"))
                .thenReturn(Optional.empty());

        service.ingestCollected(1L, VacancySource.REMOTIVE, raw);

        verify(repository).findByUserIdAndSourceAndExternalId(1L, VacancySource.REMOTIVE, "https://x.com/1");
    }

    @Test
    void markStale_shouldFlagOldVacancies() {
        Vacancy old = new Vacancy();
        old.setId(9L);
        old.setRemoved(false);
        when(repository.findByUserIdAndSourceAndRemovedFalseAndLastSeenAtBefore(
                eq(1L), eq(VacancySource.REMOTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(old));

        service.markStale(1L, VacancySource.REMOTIVE);

        assertTrue(old.isRemoved());
        verify(repository).saveAll(anyList());
    }
}
