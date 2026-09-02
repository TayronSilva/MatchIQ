package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vagas BR: murais da comunidade no GitHub (backend-br/vagas, frontendbr/vagas, etc.).
 * Lê as issues abertas; o corpo da issue é a descrição em Markdown.
 * Não recebe termo de busca: cada repositório já é uma seleção.
 */
@Service
public class VagasBrCollector implements JobBoardCollector {

    private static final List<String> DEFAULT_REPOS = List.of(
            "backend-br/vagas",
            "frontendbr/vagas",
            "react-brasil/vagas",
            "androiddevbr/vagas",
            "phpdevbr/vagas",
            "datascience-br/vagas"
    );

    private static final Pattern LEADING_TAG = Pattern.compile("^\\[([^\\]]+)\\]\\s*");

    private final List<String> repos;
    private final String githubApiBase;

    public VagasBrCollector() {
        this(DEFAULT_REPOS, "https://api.github.com");
    }

    public VagasBrCollector(List<String> repos) {
        this(repos, "https://api.github.com");
    }

    public VagasBrCollector(List<String> repos, String githubApiBase) {
        this.repos = repos;
        this.githubApiBase = githubApiBase;
    }

    @Override
    public VacancySource source() {
        return VacancySource.VAGASBR;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        for (String repo : repos) {
            try {
                String url = githubApiBase + "/repos/" + repo + "/issues?state=open&per_page=100";
                String body = http.get(url);
                JsonNode issues = CollectorSupport.mapper().readTree(body);
                if (issues == null || !issues.isArray()) {
                    continue;
                }
                for (JsonNode issue : issues) {
                    if (issue.has("pull_request")) {
                        continue;
                    }
                    String id = CollectorSupport.str(issue, "id");
                    String titleRaw = CollectorSupport.str(issue, "title");
                    String description = CollectorSupport.str(issue, "body");
                    String htmlUrl = CollectorSupport.str(issue, "html_url");
                    if (description == null || description.isBlank()) {
                        continue;
                    }
                    WorkModality modality = null;
                    String location = null;
                    String title = titleRaw;
                    Matcher m = LEADING_TAG.matcher(titleRaw == null ? "" : titleRaw.trim());
                    if (m.find()) {
                        String tag = m.group(1).trim().toLowerCase();
                        if (tag.contains("remoto")) {
                            modality = WorkModality.REMOTE;
                        } else if (tag.contains("híbrido") || tag.contains("hibrido")) {
                            modality = WorkModality.HYBRID;
                        } else if (tag.contains("presencial")) {
                            modality = WorkModality.ONSITE;
                        } else {
                            location = m.group(1).trim();
                        }
                        title = titleRaw.replaceFirst("^\\[[^\\]]+\\]\\s*", "").trim();
                    }
                    out.add(new RawVacancy(id, title, null, description, htmlUrl, location,
                            modality, null, CollectorSupport.parseIso(CollectorSupport.str(issue, "created_at"))));
                }
            } catch (Exception ignored) {
                // falha num repositório não derruba os demais
            }
        }
        return out;
    }
}
