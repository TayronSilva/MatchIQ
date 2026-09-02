package com.matchiq.vacancy.service;

import com.matchiq.analysis.repository.AnalysisRepository;
import com.matchiq.application.repository.ApplicationRepository;
import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.recommendation.repository.RecommendationRepository;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.skill.service.SkillExtractorService;
import com.matchiq.tailor.repository.ResumeSessionRepository;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.dto.VacancyResponse.VacancySkillResponse;
import com.matchiq.vacancy.mapper.VacancyMapper;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import com.matchiq.vacancy.collector.RawVacancy;
import com.matchiq.vacancy.domain.VacancySource;
import com.matchiq.vacancy.service.ScrapedVacancy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VacancyService {

    private static final int MIN_DESCRIPTION_LENGTH = 300;
    private static final int STALE_DAYS = 21;

    private final VacancyRepository repository;
    private final VacancySkillRepository vacancySkillRepository;
    private final SkillRepository skillRepository;
    private final SkillExtractorService skillExtractor;
    private final VacancyMapper mapper;
    private final VacancyScraper scraper;
    private final MatchRepository matchRepository;
    private final AnalysisRepository analysisRepository;
    private final RecommendationRepository recommendationRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeSessionRepository resumeSessionRepository;

    @Transactional
    public VacancyResponse create(Long userId, CreateVacancyRequest request) {
        Vacancy vacancy = mapper.toEntity(userId, request);
        Vacancy saved = repository.save(vacancy);
        linkExtractedSkills(saved.getId(), skillExtractor.extract(saved.getDescription()));
        return toResponseWithSkills(saved);
    }

    @Transactional
    public VacancyResponse createFromUrl(Long userId, String url) {
        ScrapedVacancy scraped = scraper.scrape(url);

        Vacancy vacancy = new Vacancy();
        vacancy.setUserId(userId);
        vacancy.setTitle(scraped.title() == null || scraped.title().isBlank() ? "Vaga" : scraped.title());
        vacancy.setDescription(scraped.description() == null || scraped.description().isBlank() ? "" : scraped.description());
        vacancy.setUrl(url);
        vacancy.setSource(com.matchiq.vacancy.domain.VacancySource.URL);

        Vacancy saved = repository.save(vacancy);
        linkExtractedSkills(saved.getId(), skillExtractor.extract(saved.getDescription()));
        VacancyResponse response = toResponseWithSkills(saved);
        response.setNeedsMoreInfo(saved.getDescription() == null || saved.getDescription().length() < MIN_DESCRIPTION_LENGTH);
        return response;
    }

    @Transactional(readOnly = true)
    public List<VacancyResponse> findByUserId(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(v -> !v.isRemoved())
                .map(this::toResponseWithSkills)
                .toList();
    }

    @Transactional(readOnly = true)
    public VacancyResponse findByIdAndUserId(Long id, Long userId) {
        Vacancy vacancy = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));
        return toResponseWithSkills(vacancy);
    }

    @Transactional
    public VacancyResponse update(Long id, Long userId, UpdateVacancyRequest request) {
        Vacancy vacancy = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));

        mapper.updateEntity(vacancy, request);
        Vacancy updated = repository.save(vacancy);

        vacancySkillRepository.deleteByVacancyId(updated.getId());
        linkExtractedSkills(updated.getId(), skillExtractor.extract(updated.getDescription()));

        return toResponseWithSkills(updated);
    }

    @Transactional
    public VacancyResponse favorite(Long id, Long userId, boolean favorite) {
        Vacancy vacancy = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));
        vacancy.setFavorite(favorite);
        Vacancy updated = repository.save(vacancy);
        return toResponseWithSkills(updated);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Vacancy vacancy = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));
        cascadeDeleteByVacancyId(id);
        repository.delete(vacancy);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        List<Vacancy> vacancies = repository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Vacancy vacancy : vacancies) {
            cascadeDeleteByVacancyId(vacancy.getId());
        }
        repository.deleteByUserId(userId);
    }

    private void cascadeDeleteByVacancyId(Long vacancyId) {
        vacancySkillRepository.deleteByVacancyId(vacancyId);
        applicationRepository.deleteByVacancyId(vacancyId);
        resumeSessionRepository.deleteByVacancyId(vacancyId);
        List<Match> matches = matchRepository.findByVacancyId(vacancyId);
        for (Match match : matches) {
            analysisRepository.deleteByMatchId(match.getId());
            recommendationRepository.deleteByMatchId(match.getId());
        }
        matchRepository.deleteByVacancyId(vacancyId);
    }

    @Transactional
    public IngestionOutcome ingestCollected(Long userId, VacancySource source, RawVacancy raw) {
        String externalId = (raw.externalId() == null || raw.externalId().isBlank())
                ? raw.url() : raw.externalId();
        if (externalId == null || externalId.isBlank()) {
            return IngestionOutcome.UPDATED;
        }

        Optional<Vacancy> existing = repository.findByUserIdAndSourceAndExternalId(userId, source, externalId);
        if (existing.isPresent()) {
            Vacancy v = existing.get();
            String newDescription = raw.description() == null ? v.getDescription() : raw.description();
            boolean descriptionChanged = !newDescription.equals(v.getDescription());
            v.setTitle(raw.title() == null || raw.title().isBlank() ? v.getTitle() : raw.title());
            v.setDescription(newDescription);
            v.setCompany(raw.company() == null ? v.getCompany() : raw.company());
            v.setLocation(raw.location() == null ? v.getLocation() : raw.location());
            v.setWorkModality(raw.workModality() == null ? v.getWorkModality() : raw.workModality());
            v.setSalaryRange(raw.salaryRange() == null ? v.getSalaryRange() : raw.salaryRange());
            v.setUrl(raw.url() == null ? v.getUrl() : raw.url());
            v.setLastSeenAt(LocalDateTime.now());
            v.setRemoved(false);
            repository.save(v);
            if (descriptionChanged) {
                vacancySkillRepository.deleteByVacancyId(v.getId());
                linkExtractedSkills(v.getId(), skillExtractor.extract(v.getDescription()));
            }
            return IngestionOutcome.UPDATED;
        }

        Vacancy v = new Vacancy();
        v.setUserId(userId);
        v.setTitle(raw.title() == null || raw.title().isBlank() ? "Vaga" : raw.title());
        v.setDescription(raw.description() == null ? "" : raw.description());
        v.setCompany(raw.company());
        v.setLocation(raw.location());
        v.setWorkModality(raw.workModality());
        v.setSalaryRange(raw.salaryRange());
        v.setUrl(raw.url());
        v.setSource(source);
        v.setExternalId(externalId);
        v.setLastSeenAt(LocalDateTime.now());
        v.setRemoved(false);
        Vacancy saved = repository.save(v);
        linkExtractedSkills(saved.getId(), skillExtractor.extract(saved.getDescription()));
        return IngestionOutcome.CREATED;
    }

    @Transactional
    public void markStale(Long userId, VacancySource source) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(STALE_DAYS);
        List<Vacancy> stale = repository.findByUserIdAndSourceAndRemovedFalseAndLastSeenAtBefore(
                userId, source, threshold);
        for (Vacancy v : stale) {
            v.setRemoved(true);
        }
        if (!stale.isEmpty()) {
            repository.saveAll(stale);
        }
    }

    private void linkExtractedSkills(Long vacancyId, List<String> skillNames) {
        for (String name : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> skillRepository.save(createSkill(name)));

            if (!vacancySkillRepository.existsByVacancyIdAndSkillId(vacancyId, skill.getId())) {
                VacancySkill vacancySkill = new VacancySkill();
                vacancySkill.setVacancyId(vacancyId);
                vacancySkill.setSkillId(skill.getId());
                vacancySkillRepository.save(vacancySkill);
            }
        }
    }

    private Skill createSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skill;
    }

    private VacancyResponse toResponseWithSkills(Vacancy vacancy) {
        List<VacancySkillResponse> skills = vacancySkillRepository.findByVacancyId(vacancy.getId())
                .stream()
                .map(vs -> {
                    Skill skill = skillRepository.findById(vs.getSkillId())
                            .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + vs.getSkillId()));
                    VacancySkillResponse r = new VacancySkillResponse();
                    r.setSkillId(skill.getId());
                    r.setName(skill.getName());
                    r.setLevel(vs.getLevel());
                    return r;
                })
                .toList();
        return mapper.toResponse(vacancy, skills);
    }
}
