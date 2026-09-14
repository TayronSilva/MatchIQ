package com.matchiq.vacancy.collector;

import java.util.*;

public final class SkillToRepoMapper {

    private SkillToRepoMapper() {
    }

    private static final Map<String, List<String>> SKILL_TO_REPOS = Map.ofEntries(
            Map.entry("java", List.of("backend-br/vagas", "dotnetdevbr/vagas")),
            Map.entry("spring boot", List.of("backend-br/vagas")),
            Map.entry("spring framework", List.of("backend-br/vagas")),
            Map.entry("spring security", List.of("backend-br/vagas")),
            Map.entry("hibernate", List.of("backend-br/vagas")),
            Map.entry("jpa", List.of("backend-br/vagas")),
            Map.entry("python", List.of("pythonbrasil/vagas", "datascience-br/vagas")),
            Map.entry("django", List.of("pythonbrasil/vagas")),
            Map.entry("flask", List.of("pythonbrasil/vagas")),
            Map.entry("fastapi", List.of("pythonbrasil/vagas")),
            Map.entry("javascript", List.of("jsbrasil/vagas", "frontendbr/vagas")),
            Map.entry("typescript", List.of("jsbrasil/vagas", "frontendbr/vagas")),
            Map.entry("react", List.of("react-brasil/vagas", "frontendbr/vagas")),
            Map.entry("react native", List.of("react-brasil/vagas")),
            Map.entry("angular", List.of("frontendbr/vagas")),
            Map.entry("vue.js", List.of("frontendbr/vagas")),
            Map.entry("next.js", List.of("frontendbr/vagas")),
            Map.entry("node.js", List.of("jsbrasil/vagas", "backend-br/vagas")),
            Map.entry("php", List.of("phpdevbr/vagas")),
            Map.entry("flutter", List.of("flutterbrasil/vagas")),
            Map.entry("dart", List.of("flutterbrasil/vagas")),
            Map.entry("kotlin", List.of("androiddevbr/vagas", "backend-br/vagas")),
            Map.entry("swift", List.of("androiddevbr/vagas")),
            Map.entry("c#", List.of("dotnetdevbr/vagas")),
            Map.entry("css", List.of("cssbrasil/vagas", "frontendbr/vagas")),
            Map.entry("sql", List.of("backend-br/vagas", "datascience-br/vagas")),
            Map.entry("postgresql", List.of("backend-br/vagas")),
            Map.entry("mysql", List.of("backend-br/vagas")),
            Map.entry("mongodb", List.of("backend-br/vagas")),
            Map.entry("docker", List.of("backend-br/vagas", "devopsbr/vagas")),
            Map.entry("kubernetes", List.of("kubernetes-br/vagas", "devopsbr/vagas")),
            Map.entry("devops", List.of("devopsbr/vagas")),
            Map.entry("machine learning", List.of("datascience-br/vagas")),
            Map.entry("data science", List.of("datascience-br/vagas"))
    );

    private static final Map<String, List<String>> SKILL_TO_SEARCH_KEYWORDS = Map.ofEntries(
            Map.entry("java", List.of("java", "spring", "backend")),
            Map.entry("spring boot", List.of("spring boot", "java backend")),
            Map.entry("python", List.of("python", "django", "fastapi")),
            Map.entry("javascript", List.of("javascript", "node.js", "frontend")),
            Map.entry("typescript", List.of("typescript", "node.js", "react")),
            Map.entry("react", List.of("react", "frontend", "javascript")),
            Map.entry("angular", List.of("angular", "typescript", "frontend")),
            Map.entry("vue.js", List.of("vue", "frontend", "javascript")),
            Map.entry("node.js", List.of("node.js", "javascript", "backend")),
            Map.entry("php", List.of("php", "laravel", "backend")),
            Map.entry("c#", List.of("c#", ".net", "dotnet")),
            Map.entry("go", List.of("golang", "go", "backend")),
            Map.entry("rust", List.of("rust", "backend")),
            Map.entry("kotlin", List.of("kotlin", "android", "backend")),
            Map.entry("swift", List.of("swift", "ios", "mobile")),
            Map.entry("flutter", List.of("flutter", "dart", "mobile")),
            Map.entry("docker", List.of("docker", "devops", "kubernetes")),
            Map.entry("kubernetes", List.of("kubernetes", "k8s", "devops")),
            Map.entry("aws", List.of("aws", "cloud", "devops")),
            Map.entry("postgresql", List.of("postgresql", "sql", "database")),
            Map.entry("mongodb", List.of("mongodb", "nosql", "database")),
            Map.entry("machine learning", List.of("machine learning", "ml", "data science")),
            Map.entry("devops", List.of("devops", "ci/cd", "infrastructure"))
    );

    public static List<String> reposForSkills(Set<String> skillNames) {
        Set<String> repos = new LinkedHashSet<>();
        for (String skill : skillNames) {
            List<String> mapped = SKILL_TO_REPOS.get(skill.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                repos.addAll(mapped);
            }
        }
        if (repos.isEmpty()) {
            return List.of("backend-br/vagas", "frontendbr/vagas", "jsbrasil/vagas");
        }
        return new ArrayList<>(repos);
    }

    public static List<String> searchKeywordsForSkills(Set<String> skillNames) {
        List<String> keywords = new ArrayList<>();
        for (String skill : skillNames) {
            List<String> mapped = SKILL_TO_SEARCH_KEYWORDS.get(skill.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                keywords.addAll(mapped);
            }
        }
        if (keywords.isEmpty()) {
            keywords.addAll(skillNames);
        }
        return keywords.stream().distinct().limit(8).toList();
    }

    public static int computeRelevanceScore(String title, String description, Set<String> userSkillNames) {
        if (userSkillNames.isEmpty()) {
            return 1;
        }
        String text = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String skill : userSkillNames) {
            if (text.contains(skill.toLowerCase(Locale.ROOT))) {
                score += 10;
            }
        }
        if (score == 0) {
            return 0;
        }
        String titleLower = (title != null ? title : "").toLowerCase(Locale.ROOT);
        for (String skill : userSkillNames) {
            if (titleLower.contains(skill.toLowerCase(Locale.ROOT))) {
                score += 5;
            }
        }
        return score;
    }
}
