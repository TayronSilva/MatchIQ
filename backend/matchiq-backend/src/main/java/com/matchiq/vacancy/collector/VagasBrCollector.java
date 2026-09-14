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
@Order(1)
public class VagasBrCollector implements JobBoardCollector {

    private static final List<String> ALL_REPOS = List.of(
            "backend-br/vagas",
            "frontendbr/vagas",
            "react-brasil/vagas",
            "androiddevbr/vagas",
            "phpdevbr/vagas",
            "datascience-br/vagas",
            "pythonbrasil/vagas",
            "cssbrasil/vagas",
            "jsbrasil/vagas",
            "flutterbrasil/vagas",
            "dotnetdevbr/vagas",
            "kubernetes-br/vagas",
            "devopsbr/vagas"
    );

    private static final Pattern LEADING_TAG = Pattern.compile("^\\[([^\\]]+)\\]\\s*");

    private final List<String> repos;
    private final String githubApiBase;

    public VagasBrCollector() {
        this(ALL_REPOS, "https://api.github.com");
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
        return collectFromRepos(http, repos);
    }

    @Override
    public List<RawVacancy> collect(PoliteHttpClient http, List<String> keywords) throws Exception {
        Set<String> skillSet = keywords.stream()
                .map(k -> k.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> relevantRepos = selectReposBySkills(skillSet);

        if (relevantRepos.isEmpty()) {
            relevantRepos = repos.subList(0, Math.min(3, repos.size()));
        }

        log.info("VagasBr: repos selecionados pelas skills {} = {}", skillSet, relevantRepos);
        return collectFromRepos(http, relevantRepos);
    }

    private List<String> selectReposBySkills(Set<String> skillSet) {
        List<String> selected = new ArrayList<>();
        for (String repo : repos) {
            String repoLower = repo.toLowerCase(Locale.ROOT);
            for (String skill : skillSet) {
                if (repoLower.contains(skill) || skillMatchesRepo(skill, repoLower)) {
                    if (!selected.contains(repo)) {
                        selected.add(repo);
                    }
                    break;
                }
            }
        }
        return selected;
    }

    private boolean skillMatchesRepo(String skill, String repoLower) {
        return switch (skill) {
            case "java", "spring boot", "spring framework", "spring security",
                 "hibernate", "jpa", "quarkus", "kotlin" ->
                    repoLower.contains("backend-br");
            case "python", "django", "flask", "fastapi", "machine learning",
                 "data science", "deep learning" ->
                    repoLower.contains("pythonbrasil") || repoLower.contains("datascience");
            case "javascript", "typescript", "node.js", "express", "nestjs" ->
                    repoLower.contains("jsbrasil") || repoLower.contains("frontendbr");
            case "react", "react native", "next.js", "vue.js", "angular" ->
                    repoLower.contains("react-brasil") || repoLower.contains("frontendbr");
            case "php", "laravel" ->
                    repoLower.contains("phpdevbr");
            case "flutter", "dart" ->
                    repoLower.contains("flutterbrasil");
            case "c#", ".net", "dotnet" ->
                    repoLower.contains("dotnetdevbr");
            case "css", "sass", "tailwind css" ->
                    repoLower.contains("cssbrasil");
            case "docker", "kubernetes", "devops", "ci/cd" ->
                    repoLower.contains("kubernetes-br") || repoLower.contains("devopsbr");
            case "android", "swift" ->
                    repoLower.contains("androiddevbr");
            default -> false;
        };
    }

    private List<RawVacancy> collectFromRepos(PoliteHttpClient http, List<String> targetRepos) throws Exception {
        List<RawVacancy> out = new ArrayList<>();
        for (String repo : targetRepos) {
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
                        String tag = java.text.Normalizer.normalize(m.group(1).trim().toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
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
