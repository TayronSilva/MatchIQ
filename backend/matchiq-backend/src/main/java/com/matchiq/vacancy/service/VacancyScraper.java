package com.matchiq.vacancy.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê uma URL de vaga e extrai título e descrição.
 *
 * Estratégia (inspirada no vagas-bot):
 *  - Gupy e Remotar expõem uma API JSON pública; batemos nela direto em vez de
 *    raspar o HTML (que em SPA quase nunca vem renderizado no servidor).
 *  - Demais sites: extração de conteúdo principal do HTML com headers de
 *    navegador (User-Agent Firefox + sec-fetch), que passam em muito mais portais
 *    do que o crawler padrão do Jsoup.
 *
 * O Jsoup não executa JavaScript, então sites 100% SPA que não têm API (ex.:
 * Indeed, que ainda devolve 403 para requisições de servidor) continuam exigindo
 * que o usuário cole o texto manualmente.
 */
@Service
public class VacancyScraper {

    private static final int TIMEOUT_MS = 10000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String FIREFOX_UA =
            "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0";

    private static final Map<String, String> SITE_SELECTORS = Map.of(
            "indeed.com", "#jobDescriptionText",
            "linkedin.com", ".jobs-description, .description__jobs-description",
            "vagas.com.br", "#descricao",
            "trabalhar.com", ".vaga-descricao, .job-description"
    );

    private static final List<String> GENERIC_SELECTORS = List.of(
            "article",
            "[role='main']",
            ".job-description", ".vacancy-description", ".description",
            ".jobdetail", ".job-details", "#jobDescriptionText",
            "main"
    );

    private static final Pattern GUPY_TOKEN = Pattern.compile("/job/([A-Za-z0-9_=-]+)");
    private static final Pattern GUPY_JOB_ID = Pattern.compile("\"jobId\"\\s*:\\s*(\\d+)");
    private static final Pattern REMOTAR_PATH_ID =
            Pattern.compile("(?:vagas|jobs|vaga)[^/]*?/(\\d{4,})");
    private static final Pattern NUM_ID = Pattern.compile("(\\d{4,})");

    public ScrapedVacancy scrape(String url) {
        try {
            String host = hostOf(url);
            if (host != null) {
                if (host.contains("gupy")) {
                    return scrapeGupy(url);
                }
                if (host.contains("remotar")) {
                    return scrapeRemotar(url);
                }
            }
            return scrapeHtml(url);
        } catch (VacancyScrapeException e) {
            throw e;
        } catch (Exception e) {
            throw new VacancyScrapeException("Não foi possível ler a vaga a partir da URL: " + url);
        }
    }

    private ScrapedVacancy scrapeGupy(String url) {
        String id = extractGupyId(url);
        String api = "https://employability-portal.gupy.io/api/v1/jobs/" + id;
        JsonNode node = fetchJson(api, gupyHeaders());
        String title = text(node, "name");
        String description = stripHtml(text(node, "description"));
        return new ScrapedVacancy(title.isBlank() ? "Vaga" : title.trim(), description);
    }

    private ScrapedVacancy scrapeRemotar(String url) {
        String id = extractRemotarId(url);
        String api = "https://api.remotar.com.br/jobs/" + id;
        JsonNode node = fetchJson(api, remotarHeaders());
        String title = text(node, "title");
        String description = stripHtml(text(node, "description"));
        return new ScrapedVacancy(title.isBlank() ? "Vaga" : title.trim(), description);
    }

    private JsonNode fetchJson(String apiUrl, Map<String, String> headers) {
        try {
            Connection conn = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MS)
                    .userAgent(FIREFOX_UA)
                    .followRedirects(true);
            headers.forEach(conn::header);
            String body = conn.execute().body();
            return OBJECT_MAPPER.readTree(body);
        } catch (Exception e) {
            throw new VacancyScrapeException("Não foi possível ler a vaga na API: " + apiUrl);
        }
    }

    private ScrapedVacancy scrapeHtml(String url) {
        try {
            Connection conn = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent(FIREFOX_UA)
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Cache-Control", "no-cache")
                    .header("DNT", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .followRedirects(true);
            Document doc = conn.get();

            if (isBlocked(doc)) {
                throw new VacancyScrapeException("BLOCKED");
            }

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

            String body = extractMainText(doc, url);
            if (body != null && body.length() > description.length()) {
                description = body;
            }

            description = clean(description);

            return new ScrapedVacancy(title == null ? "Vaga" : title.trim(), description);
        } catch (VacancyScrapeException e) {
            throw e;
        } catch (org.jsoup.HttpStatusException hse) {
            if (hse.getStatusCode() == 403 || hse.getStatusCode() == 429) {
                throw new VacancyScrapeException("BLOCKED");
            }
            throw new VacancyScrapeException("Não foi possível ler a vaga a partir da URL: " + url);
        } catch (Exception e) {
            throw new VacancyScrapeException("Não foi possível ler a vaga a partir da URL: " + url);
        }
    }

    private String extractGupyId(String url) {
        Matcher m = GUPY_TOKEN.matcher(url);
        if (m.find()) {
            try {
                byte[] decoded = tryDecodeBase64(m.group(1));
                String s = new String(decoded, StandardCharsets.UTF_8);
                Matcher idm = GUPY_JOB_ID.matcher(s);
                if (idm.find()) {
                    return idm.group(1);
                }
            } catch (Exception ignored) {
            }
        }
        Matcher num = NUM_ID.matcher(url);
        if (num.find()) {
            return num.group(1);
        }
        throw new VacancyScrapeException("Não foi possível identificar o ID da vaga Gupy na URL: " + url);
    }

    private String extractRemotarId(String url) {
        Matcher m = REMOTAR_PATH_ID.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        Matcher any = NUM_ID.matcher(url);
        if (any.find()) {
            return any.group(1);
        }
        throw new VacancyScrapeException("Não foi possível identificar o ID da vaga Remotar na URL: " + url);
    }

    private byte[] tryDecodeBase64(String token) {
        try {
            return Base64.getUrlDecoder().decode(token);
        } catch (Exception e) {
            return Base64.getDecoder().decode(token);
        }
    }

    private Map<String, String> gupyHeaders() {
        return Map.of(
                "Accept", "application/json, text/plain, */*",
                "Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
                "Cache-Control", "no-cache",
                "DNT", "1",
                "Sec-Fetch-Dest", "empty",
                "Sec-Fetch-Mode", "cors",
                "Sec-Fetch-Site", "cross-site",
                "Origin", "https://portal.gupy.io",
                "Referer", "https://portal.gupy.io/"
        );
    }

    private Map<String, String> remotarHeaders() {
        return Map.of(
                "Accept", "application/json, text/plain, */*",
                "Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
                "Cache-Control", "no-cache",
                "DNT", "1",
                "Sec-Fetch-Dest", "empty",
                "Sec-Fetch-Mode", "cors",
                "Sec-Fetch-Site", "cross-site",
                "Referer", "https://remotar.com.br/"
        );
    }

    private String extractMainText(Document doc, String url) {
        String host = hostOf(url);
        if (host != null) {
            for (Map.Entry<String, String> entry : SITE_SELECTORS.entrySet()) {
                if (host.contains(entry.getKey())) {
                    String text = bestText(doc.select(entry.getValue()));
                    if (text != null) {
                        return text;
                    }
                }
            }
        }
        for (String selector : GENERIC_SELECTORS) {
            String text = bestText(doc.select(selector));
            if (text != null && text.length() > 200) {
                return text;
            }
        }
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
            return new URI(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String metaContent(Document doc, String property) {
        Element meta = doc.selectFirst(
                "meta[property=\"" + property + "\"], meta[name=\"" + property + "\"]");
        return meta == null ? null : meta.attr("content");
    }

    private boolean isBlocked(Document doc) {
        String text = (doc.title() + " " + doc.text()).toLowerCase(Locale.ROOT);
        return text.contains("captcha") || text.contains("verify you are human")
                || text.contains("acesso negado") || text.contains("access denied")
                || text.contains("unusual traffic")
                || (text.contains("robot") && text.length() < 500);
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null ? "" : v.asText("").trim();
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ")
                .replaceAll("(?i)candidatar-se|apply now|aplicar", "")
                .trim();
    }
}
