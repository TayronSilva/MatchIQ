package com.matchiq.vacancy.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Lê uma URL de vaga e extrai título e descrição a partir das meta tags (og:title,
 * og:description, twitter:description, meta[name=description]).
 * Funciona para portais que expõem preview; não executa JavaScript.
 */
@Service
public class VacancyScraper {

    private static final int TIMEOUT_MS = 8000;

    public ScrapedVacancy scrape(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) MatchIQ-Bot/1.0")
                    .followRedirects(true)
                    .get();

            String title = metaContent(doc, "og:title");
            if (title == null || title.isBlank()) {
                title = doc.title();
            }

            String description = metaContent(doc, "og:description");
            if (description == null || description.isBlank()) {
                description = metaContent(doc, "twitter:description");
            }
            if (description == null || description.isBlank()) {
                description = metaContent(doc, "description");
            }

            return new ScrapedVacancy(title, description);
        } catch (IOException | IllegalArgumentException e) {
            throw new VacancyScrapeException("Não foi possível ler a vaga a partir da URL: " + url);
        }
    }

    private String metaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=\"" + property + "\"], meta[name=\"" + property + "\"]");
        return meta == null ? null : meta.attr("content");
    }

    public record ScrapedVacancy(String title, String description) {
    }
}
