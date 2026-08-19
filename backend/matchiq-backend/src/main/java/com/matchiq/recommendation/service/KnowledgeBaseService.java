package com.matchiq.recommendation.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Carrega a base de conhecimento (modelo de currículo + guia da Gupy)
 * dos arquivos em src/main/resources/knowledge e a disponibiliza para
 * enriquecer os prompts da IA.
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private String resumeTemplate = "";
    private String gupyGuide = "";

    @PostConstruct
    void load() {
        resumeTemplate = loadResource("knowledge/resume-template.txt");
        gupyGuide = loadResource("knowledge/gupy-guide.txt");
    }

    private String loadResource(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Knowledge base file not found: {}", path);
            return "";
        }
    }

    public String resumeTemplate() {
        return resumeTemplate;
    }

    public String gupyGuide() {
        return gupyGuide;
    }

    public boolean isEmpty() {
        return resumeTemplate.isBlank() && gupyGuide.isBlank();
    }
}
