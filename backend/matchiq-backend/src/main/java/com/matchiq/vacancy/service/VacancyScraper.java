package com.matchiq.vacancy.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Lê uma URL de vaga e extrai título e descrição.
 *
 * Diferente da abordagem anterior (que só lia meta tags), aqui também extraímos
 * o conteúdo principal da página: a descrição real da vaga quase nunca vem em
 * og:description — ela fica no corpo do HTML. Sem isso, o usuário era obrigado a
 * colar o texto manualmente.
 *
 * O Jsoup não executa JavaScript, então sites 100% SPA (que renderizam a vaga via
 * JS) ainda podem vir curtos — nesse caso mantemos o aviso de "cole o texto".
 */
@Service
public class VacancyScraper {

    private static final int TIMEOUT_MS = 10000;

    /** Seletores específicos por domínio para pegar o corpo da vaga direto. */
    private static final Map<String, String> SITE_SELECTORS = Map.of(
            "indeed.com", "#jobDescriptionText",
            "linkedin.com", ".jobs-description, .description__jobs-description",
            "gupy.io", "[data-testid='job-description'], .sc-...job-description",
            "vagas.com.br", "#descricao",
            "trabalhar.com", ".vaga-descricao, .job-description"
    );

    /** Seletores genéricos, na ordem em que tentamos. */
    private static final List<String> GENERIC_SELECTORS = List.of(
            "article",
            "[role='main']",
            ".job-description", ".vacancy-description", ".description",
            ".jobdetail", ".job-details", "#jobDescriptionText",
            "main"
    );

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

            // O conteúdo principal costuma ser muito mais rico que a meta tag.
            String body = extractMainText(doc, url);
            if (body != null && body.length() > description.length()) {
                description = body;
            }

            description = clean(description);

            return new ScrapedVacancy(title == null ? "Vaga" : title.trim(), description);
        } catch (IOException | IllegalArgumentException e) {
            throw new VacancyScrapeException("Não foi possível ler a vaga a partir da URL: " + url);
        }
    }

    private String extractMainText(Document doc, String url) {
        String host = hostOf(url);

        // 1) seletor específico do site
        if (host != null) {
            for (Map.Entry<String, String> entry : SITE_SELECTORS.entrySet()) {
                if (host.contains(entry.getKey())) {
                    Elements els = doc.select(entry.getValue());
                    String text = bestText(els);
                    if (text != null) {
                        return text;
                    }
                }
            }
        }

        // 2) seletores genéricos
        for (String selector : GENERIC_SELECTORS) {
            Elements els = doc.select(selector);
            String text = bestText(els);
            if (text != null && text.length() > 200) {
                return text;
            }
        }

        // 3) fallback: maior bloco de texto dentre divs/sections
        return largestTextBlock(doc);
    }

    private String bestText(Elements els) {
        if (els == null || els.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Element el : els) {
            sb.append(el.text()).append("\n");
        }
        String text = sb.toString().trim();
        return text.isBlank() ? null : text;
    }

    private String largestTextBlock(Document doc) {
        int max = 0;
        String best = null;
        for (Element el : doc.select("div, section")) {
            String text = el.ownText();
            if (text.length() > max) {
                max = text.length();
                best = text;
            }
        }
        return best;
    }

    private String hostOf(String url) {
        try {
            return new java.net.URI(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String metaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=\"" + property + "\"], meta[name=\"" + property + "\"]");
        return meta == null ? null : meta.attr("content");
    }

    /** Remove ruídos comuns (menus, "candidatar-se", espaços repetidos). */
    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ")
                .replaceAll("(?i)candidatar-se|apply now|aplicar", "")
                .trim();
    }

    public record ScrapedVacancy(String title, String description) {
    }
}
