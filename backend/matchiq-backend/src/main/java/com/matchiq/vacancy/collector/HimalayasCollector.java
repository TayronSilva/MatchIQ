package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Himalayas remote jobs API (https://himalayas.app/jobs/api).
 * Vagas remotas; a resposta traz "excerpt" (primeiros parágrafos) como descrição.
 */
@Service
public class HimalayasCollector implements JobBoardCollector {

    private final String url;

    public HimalayasCollector() {
        this("https://himalayas.app/jobs/api?limit=20");
    }

    public HimalayasCollector(String url) {
        this.url = url;
    }

    @Override
    public VacancySource source() {
        return VacancySource.HIMALAYAS;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        String body = http.get(url);
        JsonNode root = CollectorSupport.mapper().readTree(body);
        JsonNode jobs = root.get("jobs");
        if (jobs == null) {
            return out;
        }
        for (JsonNode j : jobs) {
            String id = CollectorSupport.str(j, "guid");
            String title = CollectorSupport.str(j, "title");
            String company = CollectorSupport.str(j, "companyName");
            String description = CollectorSupport.str(j, "description");
            if (description == null) {
                description = CollectorSupport.str(j, "excerpt");
            }
            String u = CollectorSupport.str(j, "applicationLink");
            String salary = CollectorSupport.buildSalary(
                    CollectorSupport.str(j, "minSalary"), CollectorSupport.str(j, "maxSalary"));
            String period = CollectorSupport.str(j, "salaryPeriod");
            if (salary != null && period != null) {
                salary = salary + " / " + period;
            }
            Long pubDate = j.has("pubDate") ? j.get("pubDate").asLong() : null;
            out.add(new RawVacancy(id, title, company, description, u, null,
                    WorkModality.REMOTE, salary, CollectorSupport.fromEpochSeconds(pubDate)));
        }
        return out;
    }
}
