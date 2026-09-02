package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * RemoteOK public JSON feed (https://remoteok.com/api).
 * O primeiro elemento do array é metadados; as vagas começam no índice 1.
 * Cada vaga traz a descrição completa em HTML. Máx. ~100 vagas mais recentes.
 */
@Service
public class RemoteOkCollector implements JobBoardCollector {

    private final String url;

    public RemoteOkCollector() {
        this("https://remoteok.com/api");
    }

    public RemoteOkCollector(String url) {
        this.url = url;
    }

    @Override
    public VacancySource source() {
        return VacancySource.REMOTEOK;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        String body = http.get(url);
        JsonNode root = CollectorSupport.mapper().readTree(body);
        if (!root.isArray()) {
            return out;
        }
        for (int i = 1; i < root.size(); i++) {
            JsonNode j = root.get(i);
            String id = CollectorSupport.str(j, "id");
            String title = CollectorSupport.str(j, "position");
            String company = CollectorSupport.str(j, "company");
            String description = CollectorSupport.htmlToText(CollectorSupport.str(j, "description"));
            String u = CollectorSupport.str(j, "url");
            String location = CollectorSupport.str(j, "location");
            String salary = CollectorSupport.buildSalary(
                    CollectorSupport.str(j, "salary_min"), CollectorSupport.str(j, "salary_max"));
            out.add(new RawVacancy(id, title, company, description, u, location,
                    WorkModality.REMOTE, salary, CollectorSupport.parseIso(CollectorSupport.str(j, "date"))));
        }
        return out;
    }
}
