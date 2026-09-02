package com.matchiq.vacancy.collector;

import com.matchiq.vacancy.domain.VacancySource;

/**
 * Resultado de uma rodada de coleta de um portal específico.
 */
public record CollectorResult(
        VacancySource source,
        int fetched,
        int created,
        int updated,
        boolean success,
        String error) {

    public static CollectorResult failed(VacancySource source, String error) {
        return new CollectorResult(source, 0, 0, 0, false, error);
    }

    public static CollectorResult ok(VacancySource source, int fetched, int created, int updated) {
        return new CollectorResult(source, fetched, created, updated, true, null);
    }
}
