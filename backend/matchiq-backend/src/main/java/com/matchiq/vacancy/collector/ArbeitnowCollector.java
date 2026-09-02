package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Arbeitnow job board API (https://www.arbeitnow.com/api/job-board-api).
 * Retorna vagas (remotas e europeias) com descrição em HTML e flag "remote".
 */
@Service
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
        List<RawVacancy> out = new ArrayList<>();
        String body = http.get(url);
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
}
