package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import java.time.LocalDateTime;

/**
 * Vagas cruas retornadas por um {@link JobBoardCollector}, antes de virarem entidades.
 */
public record RawVacancy(
        String externalId,
        String title,
        String company,
        String description,
        String url,
        String location,
        WorkModality workModality,
        String salaryRange,
        LocalDateTime postedAt) {
}
