package com.matchiq.vacancy.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.skill.service.SkillExtractorService;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.vacancy.dto.CreateVacancyRequest;
import com.matchiq.vacancy.dto.UpdateVacancyRequest;
import com.matchiq.vacancy.dto.VacancyResponse;
import com.matchiq.vacancy.dto.VacancyResponse.VacancySkillResponse;
import com.matchiq.vacancy.mapper.VacancyMapper;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import com.matchiq.vacancy.service.VacancyScraper.ScrapedVacancy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VacancyService {

    private static final int MIN_DESCRIPTION_LENGTH = 300;

    private final VacancyRepository repository;
    private final VacancySkillRepository vacancySkillRepository;
    private final SkillRepository skillRepository;
    private final SkillExtractorService skillExtractor;
    private final VacancyMapper mapper;
    private final VacancyScraper scraper;

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
    public void delete(Long id, Long userId) {
        Vacancy vacancy = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with id: " + id));
        repository.delete(vacancy);
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
