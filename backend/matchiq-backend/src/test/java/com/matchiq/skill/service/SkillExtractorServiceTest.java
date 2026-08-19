package com.matchiq.skill.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillExtractorServiceTest {

    private final SkillExtractorService extractor = new SkillExtractorService();

    @Test
    void extract_shouldFindSkillsCaseInsensitive() {
        List<String> found = extractor.extract("Experiencia com java, spring boot e postgresql");

        assertTrue(found.contains("Java"));
        assertTrue(found.contains("Spring Boot"));
        assertTrue(found.contains("PostgreSQL"));
    }

    @Test
    void extract_shouldNotMatchPartialWords() {
        // "Java" não deve casar com "JavaScript"
        List<String> found = extractor.extract("Trabalho com JavaScript e Typescript");

        assertFalse(found.contains("Java"));
        assertTrue(found.contains("JavaScript"));
        assertTrue(found.contains("TypeScript"));
    }

    @Test
    void extract_shouldMatchCompoundTerms() {
        List<String> found = extractor.extract("Uso Spring Boot, Spring Security e GitHub Actions");

        assertTrue(found.contains("Spring Boot"));
        assertTrue(found.contains("Spring Security"));
        assertTrue(found.contains("GitHub Actions"));
    }

    @Test
    void extract_shouldReturnEmptyForNull() {
        assertTrue(extractor.extract(null).isEmpty());
    }

    @Test
    void extract_shouldReturnEmptyForBlank() {
        assertTrue(extractor.extract("   ").isEmpty());
    }

    @Test
    void extract_shouldReturnEmptyWhenNoSkillMatches() {
        assertTrue(extractor.extract("Gosto de cozinhar e tocar violão").isEmpty());
    }

    @Test
    void extract_shouldReturnCanonicalNames() {
        List<String> found = extractor.extract("spring boot, POSTGRESQL, docker");

        assertTrue(found.contains("Spring Boot"));
        assertTrue(found.contains("PostgreSQL"));
        assertTrue(found.contains("Docker"));
        // nenhum nome em lowercase cru
        assertFalse(found.contains("spring boot"));
    }
}
