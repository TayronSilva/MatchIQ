package com.matchiq.skill.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extrai skills do texto de um currículo comparando com o catálogo.
 * Retorna os nomes na forma canônica do catálogo (ex: "Spring Boot", "PostgreSQL").
 */
@Service
public class SkillExtractorService {

    public List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();

        for (String normalizedSkill : SkillCatalog.normalizedToCanonical().keySet()) {
            String canonical = SkillCatalog.match(normalized, normalizedSkill);
            if (canonical != null) {
                found.add(canonical);
            }
        }

        return found;
    }
}
