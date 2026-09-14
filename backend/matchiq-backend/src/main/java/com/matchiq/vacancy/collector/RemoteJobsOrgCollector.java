package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
@Order(5)
public class RemoteJobsOrgCollector implements JobBoardCollector {

    private final String baseUrl;

    public RemoteJobsOrgCollector() {
        this("https://remotejobs.org/api/v1/jobs");
    }

    public RemoteJobsOrgCollector(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public VacancySource source() {
        return VacancySource.REMOTEJOBSORG;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchJobs(http, baseUrl + "?category=programming&limit=50");
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        if (keywords == null || keywords.isEmpty()) {
            return collect(http);
        }
        List<RawVacancy> all = new ArrayList<>();
        String[] categories = mapKeywordsToCategories(keywords);
        for (String cat : categories) {
            try {
                String url = baseUrl + "?category=" + cat + "&limit=50";
                all.addAll(fetchJobs(http, url));
            } catch (Exception ignored) {
            }
        }
        if (all.isEmpty()) {
            return collect(http);
        }
        return deduplicate(all);
    }

    private String[] mapKeywordsToCategories(List<String> keywords) {
        List<String> lower = keywords.stream().map(String::toLowerCase).toList();
        java.util.Set<String> cats = new java.util.LinkedHashSet<>();
        for (String k : lower) {
            if (k.contains("java") || k.contains("python") || k.contains("php") || k.contains("go") || k.contains("rust")) {
                cats.add("programming");
            }
            if (k.contains("react") || k.contains("angular") || k.contains("vue") || k.contains("frontend") || k.contains("css")) {
                cats.add("design");
            }
            if (k.contains("devops") || k.contains("docker") || k.contains("kubernetes") || k.contains("aws")) {
                cats.add("programming");
            }
            if (k.contains("marketing") || k.contains("seo")) {
                cats.add("marketing");
            }
        }
        if (cats.isEmpty()) {
            cats.add("programming");
        }
        return cats.toArray(new String[0]);
    }

    private List<RawVacancy> fetchJobs(PoliteHttpClient http, String url) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        String body = http.get(url);
        JsonNode root = CollectorSupport.mapper().readTree(body);
        JsonNode jobs = root.get("data");
        if (jobs == null || !jobs.isArray()) {
            return out;
        }
        for (JsonNode job : jobs) {
            String id = CollectorSupport.str(job, "id");
            String title = CollectorSupport.str(job, "title");
            String company = null;
            JsonNode companyNode = job.get("company");
            if (companyNode != null && !companyNode.isNull()) {
                company = CollectorSupport.str(companyNode, "name");
            }
            String description = CollectorSupport.str(job, "description");
            String jobUrl = CollectorSupport.str(job, "url");
            String applyUrl = CollectorSupport.str(job, "apply_url");

            if (title != null && !title.isBlank()) {
                out.add(new RawVacancy(id, title, company, description,
                        applyUrl != null ? applyUrl : jobUrl, null,
                        WorkModality.REMOTE, null, null));
            }
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
