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

        AtomicInteger totalIngested = new AtomicInteger(0);
        List<CollectorResult> results = new ArrayList<>();
        for (JobBoardCollector collector : collectors) {
            if (totalIngested.get() >= MAX_TOTAL) {
                break;
            }
            results.add(collectOne(userId, collector, totalIngested, userSkillNames, hasResume));
        }

        lastCollectionAt.put(userId, Instant.now());
        return new CollectOutcome(results, totalIngested.get(), false, 0);
    }

    private CollectorResult collectOne(Long userId, JobBoardCollector collector,
                                      AtomicInteger totalIngested,
                                      Set<String> userSkillNames, boolean hasResume) {
        PoliteHttpClient http = new PoliteHttpClient(MIN_INTERVAL_MILLIS);
        try {
            List<RawVacancy> raw = collector.collect(http);
            int created = 0;
            int updated = 0;
            int skipped = 0;
            for (RawVacancy r : raw) {
                if (totalIngested.get() >= MAX_TOTAL) {
                    break;
                }
                try {
                    if (hasResume && !hasSkillMatch(r, userSkillNames)) {
                        skipped++;
                        continue;
                    }
                    IngestionOutcome outcome = vacancyService.ingestCollected(userId, collector.source(), r);
                    if (outcome == IngestionOutcome.CREATED) {
                        created++;
                        totalIngested.incrementAndGet();
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

    private boolean hasSkillMatch(RawVacancy r, Set<String> userSkillNames) {
        String text = joinNonBlank(r.title(), r.description(), r.company());
        List<String> vacancySkills = skillExtractor.extract(text);
        for (String s : vacancySkills) {
            if (userSkillNames.contains(s)) {
                return true;
            }
        }
        return false;
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

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
