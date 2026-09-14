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
public class RemotiveCollector implements JobBoardCollector {

    private final String baseUrl;

    public RemotiveCollector() {
        this("https://remotive.com/api/remote-jobs");
    }

    public RemotiveCollector(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public VacancySource source() {
        return VacancySource.REMOTIVE;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchJobs(http, baseUrl + "?limit=50");
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            return collect(http);
        }
        List<RawVacancy> all = new ArrayList<>();
        for (String keyword : keywords) {
            try {
                String url = baseUrl + "?limit=50&search=" + java.net.URLEncoder.encode(keyword, "UTF-8");
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
            String id = CollectorSupport.str(j, "id");
            String title = CollectorSupport.str(j, "title");
            String company = CollectorSupport.str(j, "company_name");
            String description = CollectorSupport.htmlToText(CollectorSupport.str(j, "description"));
            String u = CollectorSupport.str(j, "url");
            String location = CollectorSupport.str(j, "candidate_required_location");
            String salary = CollectorSupport.str(j, "salary");
            out.add(new RawVacancy(id, title, company, description, u, location,
                    WorkModality.REMOTE, salary, null));
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
