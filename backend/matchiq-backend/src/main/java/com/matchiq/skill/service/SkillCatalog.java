package com.matchiq.skill.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catálogo de skills conhecidas para o Skill Extractor.
 * O matching é feito por substring (case-insensitive) sobre o texto extraído do currículo.
 * Nomes com mais de uma palavra (ex: "Spring Boot") vêm antes dos nomes curtos
 * para que o extrator prefira o termo mais específico.
 */
public final class SkillCatalog {

    private SkillCatalog() {
    }

    private static final List<String> SKILLS = List.of(
            // Linguagens
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust",
            "Kotlin", "Swift", "PHP", "Ruby", "Scala", "Dart", "Shell Script",
            // Web / Frontend
            "React", "React Native", "Angular", "Vue.js", "Next.js", "Node.js",
            "HTML", "CSS", "Tailwind CSS", "SASS", "Redux", "jQuery",
            // Backend / Frameworks
            "Spring Boot", "Spring Framework", "Spring Security", "Spring Cloud",
            "Jakarta EE", "Quarkus", "Micronaut", "Hibernate", "JPA", "MyBatis",
            "Django", "Flask", "FastAPI", "Express", "NestJS", "GraphQL",
            "REST API", "Microservices", "Apache Kafka", "RabbitMQ",
            // Banco de dados
            "PostgreSQL", "MySQL", "SQL Server", "Oracle", "SQLite", "MongoDB",
            "Redis", "Elasticsearch", "Cassandra", "DynamoDB", "Firebase", "Supabase",
            "Flyway", "Liquibase",
            // Infra / DevOps / Cloud
            "Docker", "Kubernetes", "Helm", "Terraform", "Ansible", "Jenkins",
            "GitLab CI", "GitHub Actions", "AWS", "Azure", "Google Cloud Platform",
            "GCP", "Serverless", "Linux", "Nginx", "Apache Tomcat",
            // Testes
            "JUnit", "Mockito", "Selenium", "Cypress", "Playwright", "Testcontainers",
            "TDD", "BDD",
            // Ferramentas e metodologias
            "Git", "GitHub", "GitLab", "Bitbucket", "Maven", "Gradle", "Agile",
            "Scrum", "Kanban", "CI/CD", "Clean Code", "Design Patterns",
            "SOLID", "DDD", "Event-Driven Architecture",
            // Observabilidade
            "Prometheus", "Grafana", "OpenTelemetry", "New Relic", "Datadog",
            // IA / ML
            "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch",
            "Natural Language Processing", "OpenAI", "LangChain", "Ollama"
    );

    /** normalizado (lowercase) -> nome canônico. LinkedHashMap preserva a ordem da lista. */
    private static final Map<String, String> NORMALIZED_TO_CANONICAL = buildMap();

    private static Map<String, String> buildMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String skill : SKILLS) {
            map.put(skill.toLowerCase(Locale.ROOT), skill);
        }
        return map;
    }

    /**
     * Retorna o nome canônico se o texto normalizado contém a skill; caso contrário, null.
     * Para nomes com uma palavra usa contorno de palavra (ex: "Java" não casa com "JavaScript");
     * para nomes compostos usa substring simples.
     */
    public static String match(String normalizedText, String normalizedSkill) {
        if (normalizedSkill.contains(" ")) {
            return normalizedText.contains(normalizedSkill) ? NORMALIZED_TO_CANONICAL.get(normalizedSkill) : null;
        }
        return hasWordBoundary(normalizedText, normalizedSkill) ? NORMALIZED_TO_CANONICAL.get(normalizedSkill) : null;
    }

    public static Map<String, String> normalizedToCanonical() {
        return NORMALIZED_TO_CANONICAL;
    }

    private static boolean hasWordBoundary(String text, String word) {
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            boolean leftOk = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            int end = index + word.length();
            boolean rightOk = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            index = end;
        }
        return false;
    }
}
