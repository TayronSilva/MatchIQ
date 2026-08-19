package com.matchiq.vacancy.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class VacancyScraperTest {

    private static HttpServer server;
    private static String baseUrl;

    private final VacancyScraper scraper = new VacancyScraper();

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/vaga", exchange -> {
            String html = """
                    <html>
                      <head>
                        <title>Vaga de Teste</title>
                        <meta property="og:title" content="Desenvolvedor Java Sênior">
                        <meta property="og:description" content="Vaga para Java com Spring Boot e PostgreSQL">
                      </head>
                      <body>Conteúdo invisível via JS</body>
                    </html>
                    """;
            byte[] bytes = html.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/sem-meta", exchange -> {
            String html = "<html><head><title>Só o título</title></head><body>Ola</body></html>";
            byte[] bytes = html.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void scrape_shouldReadMetaTags() {
        VacancyScraper.ScrapedVacancy result = scraper.scrape(baseUrl + "/vaga");

        assertEquals("Desenvolvedor Java Sênior", result.title());
        assertEquals("Vaga para Java com Spring Boot e PostgreSQL", result.description());
    }

    @Test
    void scrape_shouldFallbackToDocumentTitle() {
        VacancyScraper.ScrapedVacancy result = scraper.scrape(baseUrl + "/sem-meta");

        assertEquals("Só o título", result.title());
        assertNull(result.description());
    }

    @Test
    void scrape_shouldThrowOnInvalidUrl() {
        assertThrows(VacancyScrapeException.class, () -> scraper.scrape("http://localhost:1/nao-existe"));
    }
}
