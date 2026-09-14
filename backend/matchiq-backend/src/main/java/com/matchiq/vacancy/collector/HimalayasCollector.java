package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
@Order(10)
public class HimalayasCollector implements JobBoardCollector {

    private final String baseUrl;

    public HimalayasCollector() {
        this("https://himalayas.app/jobs/api");
    }

    public HimalayasCollector(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public VacancySource source() {
        return VacancySource.HIMALAYAS;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchJobs(http, baseUrl + "?limit=20");
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            return collect(http);
        }
        List<RawVacancy> all = new ArrayList<>();
        for (String keyword : keywords) {
            try {
                String url = baseUrl + "?limit=20&search=" + java.net.URLEncoder.encode(keyword, "UTF-8");
                all.addAll(fetchJobs(http, url));
            } catch (Exception ignored) {
            }
        }
        return deduplicate(all);
    }

    private List<RawVacancy> fetchJobs(PoliteHttpClient http, String url) throws Exception {
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

    private List<RawVacancy> deduplicate(List<RawVacancy> vacancies) {
        List<RawVacancy> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (RawVacancy v : vacancies) {
            String key = v.externalId() != null ? v.externalId() : v.url();
            if (key != null && seen.add(key)) {
                out.add(v);
            }
        }
        return out;
    }
}
