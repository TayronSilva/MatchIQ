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
public class ArbeitnowCollector implements JobBoardCollector {

    private final String url;

    public ArbeitnowCollector() {
        this("https://www.arbeitnow.com/api/job-board-api");
    }

    public ArbeitnowCollector(String url) {
        this.url = url;
    }

    @Override
    public VacancySource source() {
        return VacancySource.ARBEITNOW;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchJobs(http, url);
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        List<RawVacancy> all = fetchJobs(http, url);
        if (keywords == null || keywords.isEmpty()) {
            return all;
        }
        return filterByKeywords(all, keywords);
    }

    private List<RawVacancy> fetchJobs(PoliteHttpClient http, String apiUrl) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        String body = http.get(apiUrl);
        JsonNode root = CollectorSupport.mapper().readTree(body);
        JsonNode data = root.get("data");
        if (data == null) {
            return out;
        }
        for (JsonNode j : data) {
            String id = CollectorSupport.str(j, "slug");
            String title = CollectorSupport.str(j, "title");
            String company = CollectorSupport.str(j, "company_name");
            String description = CollectorSupport.htmlToText(CollectorSupport.str(j, "description"));
            String u = CollectorSupport.str(j, "url");
            String location = CollectorSupport.str(j, "location");
            boolean remote = j.has("remote") && j.get("remote").asBoolean(false);
            WorkModality modality = remote ? WorkModality.REMOTE : CollectorSupport.modalityFromText(location);
            String salary = CollectorSupport.str(j, "salary");
            out.add(new RawVacancy(id, title, company, description, u, location,
                    modality, salary, CollectorSupport.parseIso(CollectorSupport.str(j, "created_at"))));
        }
        return out;
    }

    private List<RawVacancy> filterByKeywords(List<RawVacancy> vacancies, List<String> keywords) {
        List<String> lowerKeywords = keywords.stream().map(String::toLowerCase).toList();
        List<RawVacancy> matched = new ArrayList<>();
        List<RawVacancy> rest = new ArrayList<>();
        for (RawVacancy v : vacancies) {
            String text = ((v.title() != null ? v.title() : "") + " " +
                    (v.description() != null ? v.description() : "")).toLowerCase();
            boolean matches = lowerKeywords.stream().anyMatch(text::contains);
            if (matches) {
                matched.add(v);
            } else {
                rest.add(v);
            }
        }
        matched.addAll(rest);
        return matched;
    }
}
