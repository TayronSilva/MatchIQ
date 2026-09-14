package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Order(4)
public class JobicyCollector implements JobBoardCollector {

    private final String baseUrl;

    public JobicyCollector() {
        this("https://jobicy.com/api/v2/remote-jobs");
    }

    public JobicyCollector(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public VacancySource source() {
        return VacancySource.JOBICY;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return fetchJobs(http, baseUrl + "?count=50");
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        List<RawVacancy> all = fetchJobs(http, baseUrl + "?count=50");
        if (keywords == null || keywords.isEmpty()) {
            return all;
        }
        return filterByKeywords(all, keywords);
    }

    private List<RawVacancy> fetchJobs(PoliteHttpClient http, String url) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        String body = http.get(url);
        JsonNode root = CollectorSupport.mapper().readTree(body);
        JsonNode jobs = root.get("jobs");
        if (jobs == null || !jobs.isArray()) {
            return out;
        }
        for (JsonNode job : jobs) {
            String id = CollectorSupport.str(job, "jobId");
            String title = CollectorSupport.str(job, "jobTitle");
            String company = null;
            JsonNode companyNode = job.get("companyName");
            if (companyNode != null && !companyNode.isNull()) {
                company = companyNode.asText();
            }
            String description = CollectorSupport.str(job, "jobDescription");
            String jobUrl = CollectorSupport.str(job, "url");
            String location = CollectorSupport.str(job, "jobGeo");
            if (location == null || location.isBlank()) {
                location = CollectorSupport.str(job, "location");
            }

            WorkModality modality = null;
            String jobType = CollectorSupport.str(job, "jobType");
            if (jobType != null) {
                String jt = jobType.toLowerCase();
                if (jt.contains("remote")) {
                    modality = WorkModality.REMOTE;
                } else if (jt.contains("hybrid")) {
                    modality = WorkModality.HYBRID;
                } else if (jt.contains("onsite") || jt.contains("in-office")) {
                    modality = WorkModality.ONSITE;
                }
            }

            String salaryMin = CollectorSupport.str(job, "annualSalaryMin");
            String salaryMax = CollectorSupport.str(job, "annualSalaryMax");
            String salaryCurrency = CollectorSupport.str(job, "salaryCurrency");
            String salaryRange = null;
            if (salaryMin != null && salaryMax != null) {
                salaryRange = salaryCurrency + " " + salaryMin + " - " + salaryMax;
            }

            if (title != null && !title.isBlank()) {
                out.add(new RawVacancy(id, title, company, description, jobUrl, location,
                        modality, salaryRange, CollectorSupport.parseIso(CollectorSupport.str(job, "pubDate"))));
            }
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
