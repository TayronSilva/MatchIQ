package com.matchiq.vacancy.collector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente HTTP com politeness policy: identifica-se honestamente e respeita um
 * intervalo mínimo entre requisições ao mesmo host (evita rajada / bloqueio).
 *
 * Inspirado na política de coleta do Farol: User-Agent declarado, janela de
 * descanso por host e paralelismo apenas entre hosts distintos.
 */
public class PoliteHttpClient {

    private final HttpClient client;
    private final long minIntervalMillis;
    private final Map<String, Long> lastCall = new ConcurrentHashMap<>();
    private final String userAgent;

    public PoliteHttpClient(long minIntervalMillis) {
        this.minIntervalMillis = minIntervalMillis;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.userAgent = "MatchIQ/1.0 (+https://github.com/TayronSilva/MatchIQ)";
    }

    public String get(String url) throws Exception {
        String host = URI.create(url).getHost();
        synchronized (lastCall) {
            Long prev = lastCall.get(host);
            if (prev != null) {
                long wait = minIntervalMillis - (System.currentTimeMillis() - prev);
                if (wait > 0) {
                    Thread.sleep(wait);
                }
            }
            lastCall.put(host, System.currentTimeMillis());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json, application/xml, text/xml, */*")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " ao acessar " + url);
        }
        return response.body();
    }
}
