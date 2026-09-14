package com.matchiq.vacancy.service;

import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.repository.ResumeSkillRepository;
import com.matchiq.skill.repository.SkillRepository;
import com.matchiq.skill.service.SkillExtractorService;
import com.matchiq.vacancy.collector.CollectOutcome;
import com.matchiq.vacancy.collector.CollectorResult;
import com.matchiq.vacancy.collector.JobBoardCollector;
import com.matchiq.vacancy.collector.PoliteHttpClient;
import com.matchiq.vacancy.collector.RawVacancy;
import com.matchiq.vacancy.collector.SkillToRepoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyCollectorService {

    private static final long MIN_INTERVAL_MILLIS = 1500;
    private static final int MAX_TOTAL = 15;
    private static final int MAX_PER_SOURCE = 5;
    private static final int MIN_RELEVANCE_SCORE = 10;

    @Value("${matchiq.collection.cooldown-minutes:45}")
    private long cooldownMinutes;

    private final List<JobBoardCollector> collectors;
    private final VacancyService vacancyService;
    private final ResumeRepository resumeRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillExtractorService skillExtractor;

    private final ConcurrentHashMap<Long, Instant> lastCollectionAt = new ConcurrentHashMap<>();

    public CollectOutcome collectAll(Long userId) {
        Instant last = lastCollectionAt.get(userId);
        if (last != null) {
            long elapsed = java.time.Duration.between(last, Instant.now()).toMinutes();
            if (elapsed < cooldownMinutes) {
                long remaining = cooldownMinutes - elapsed;
                log.info("Cooldown ativo para usuario {}: proxima coleta em {} min", userId, remaining);
                return CollectOutcome.cooldown(remaining);
            }
        }

        Set<String> userSkillNames = loadUserSkillNames(userId);
        boolean hasResume = !userSkillNames.isEmpty();
        List<String> searchKeywords = SkillToRepoMapper.searchKeywordsForSkills(userSkillNames);

        log.info("Coletando para usuario {} com skills: {}, keywords: {}", userId, userSkillNames, searchKeywords);

        AtomicInteger totalIngested = new AtomicInteger(0);
        List<CollectorResult> results = new ArrayList<>();
        for (JobBoardCollector collector : collectors) {
            if (totalIngested.get() >= MAX_TOTAL) {
                break;
            }
            results.add(collectOne(userId, collector, totalIngested, userSkillNames, searchKeywords, hasResume));
        }

        lastCollectionAt.put(userId, Instant.now());
        return new CollectOutcome(results, totalIngested.get(), false, 0);
    }

    private CollectorResult collectOne(Long userId, JobBoardCollector collector,
                                      AtomicInteger totalIngested,
                                      Set<String> userSkillNames,
                                      List<String> searchKeywords,
                                      boolean hasResume) {
        PoliteHttpClient http = new PoliteHttpClient(MIN_INTERVAL_MILLIS);
        AtomicInteger sourceIngested = new AtomicInteger(0);
        try {
            List<RawVacancy> raw = collector.collect(http, searchKeywords);
            int created = 0;
            int updated = 0;
            int skipped = 0;

            List<RawVacancy> scored = new ArrayList<>();
            for (RawVacancy r : raw) {
                int relevance = hasResume
                        ? SkillToRepoMapper.computeRelevanceScore(r.title(), r.description(), userSkillNames)
                        : 1;
                if (hasResume && relevance < MIN_RELEVANCE_SCORE) {
                    skipped++;
                    continue;
                }
                scored.add(r);
            }

            scored.sort((a, b) -> {
                int sa = SkillToRepoMapper.computeRelevanceScore(a.title(), a.description(), userSkillNames);
                int sb = SkillToRepoMapper.computeRelevanceScore(b.title(), b.description(), userSkillNames);
                return Integer.compare(sb, sa);
            });

            for (RawVacancy r : scored) {
                if (totalIngested.get() >= MAX_TOTAL || sourceIngested.get() >= MAX_PER_SOURCE) {
                    break;
                }
                try {
                    IngestionOutcome outcome = vacancyService.ingestCollected(userId, collector.source(), r);
                    if (outcome == IngestionOutcome.CREATED) {
                        created++;
                        totalIngested.incrementAndGet();
                        sourceIngested.incrementAndGet();
                    } else if (outcome == IngestionOutcome.UPDATED) {
                        updated++;
                    }
                } catch (Exception e) {
                    log.warn("Falha ao ingerir vaga de {}: {}", collector.source(), e.getMessage());
                }
            }
            vacancyService.markStale(userId, collector.source());
            log.info("Coletor {}: {} buscadas, {} criadas, {} atualizadas, {} puladas (sem match)",
                    collector.source(), raw.size(), created, updated, skipped);
            return CollectorResult.ok(collector.source(), raw.size(), created, updated);
        } catch (Exception e) {
            log.warn("Coletor {} falhou: {}", collector.source(), e.getMessage());
            return CollectorResult.failed(collector.source(), e.getMessage());
        }
    }

    private Set<String> loadUserSkillNames(Long userId) {
        Set<String> names = new HashSet<>();
        var resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (var resume : resumes) {
            List<ResumeSkill> rskills = resumeSkillRepository.findByResumeId(resume.getId());
            for (ResumeSkill rs : rskills) {
                skillRepository.findById(rs.getSkillId())
                        .ifPresent(skill -> names.add(skill.getName()));
            }
        }
        return names;
    }
}
