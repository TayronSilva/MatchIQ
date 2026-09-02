package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Remotive public API (https://remotive.com/api/remote-jobs).
 * Retorna vagas remotas com descrição em HTML. Limite da API: no máximo 2 req/min.
 */
@Service
public class RemotiveCollector implements JobBoardCollector {

    private final String url;

    public RemotiveCollector() {
        this("https://remotive.com/api/remote-jobs?limit=50");
    }

    public RemotiveCollector(String url) {
        this.url = url;
    }

    @Override
    public VacancySource source() {
        return VacancySource.REMOTIVE;
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
}
