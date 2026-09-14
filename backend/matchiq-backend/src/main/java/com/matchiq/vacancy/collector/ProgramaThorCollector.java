package com.matchiq.vacancy.collector;

import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.VacancySource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Order(2)
public class ProgramaThorCollector implements JobBoardCollector {

    private static final List<String> ALL_REPOS = List.of(
            "comunidadejr/comunidadejr",
            "techsombra/vagas",
            "marialuisasilva/vagas",
            "guilhermesilveira/vagas",
            "dunossauro/vagas",
            "lucasamorim-dev/vagas",
            "nairton-77/vagas",
            "ana-ber/vagas"
    );

    private static final Pattern LEADING_TAG = Pattern.compile("^\\[([^\\]]+)\\]\\s*");

    private final List<String> repos;
    private final String githubApiBase;

    public ProgramaThorCollector() {
        this(ALL_REPOS, "https://api.github.com");
    }

    public ProgramaThorCollector(List<String> repos) {
        this(repos, "https://api.github.com");
    }

    public ProgramaThorCollector(List<String> repos, String githubApiBase) {
        this.repos = repos;
        this.githubApiBase = githubApiBase;
    }

    @Override
    public VacancySource source() {
        return VacancySource.PROGRAMATHOR;
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http) throws Exception {
        return collectFromRepos(http, repos);
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        return collectFromRepos(http, repos);
    }

    private List<RawVacancy> collectFromRepos(PoliteHttpClient http, List<String> targetRepos) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        for (String repo : targetRepos) {
            try {
                String url = githubApiBase + "/repos/" + repo + "/issues?state=open&per_page=50";
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
                        } else if (tag.contains("hibrido") || tag.contains("hibrido")) {
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
            }
        }
        return out;
    }
}
