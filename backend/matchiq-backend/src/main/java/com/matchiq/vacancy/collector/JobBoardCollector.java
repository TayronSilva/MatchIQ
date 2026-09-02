package com.matchiq.vacancy.collector;

import com.matchiq.vacancy.domain.VacancySource;
import java.util.List;

/**
 * Coletor de um portal de vagas específico. Cada implementação sabe como
 * buscar e normalizar as vagas de sua fonte.
 */
public interface JobBoardCollector {
    VacancySource source();

    List<RawVacancy> collect(PoliteHttpClient http) throws Exception;
}
