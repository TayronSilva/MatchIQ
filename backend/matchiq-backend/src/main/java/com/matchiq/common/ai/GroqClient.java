package com.matchiq.common.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente para a API da Groq (endpoint compatível com OpenAI Chat Completions).
 * Usa modelos Llama 3, que são rápidos e têm tier gratuito — resolve o "travamento"
 * causado pelo free router da Hugging Face.
 *
 * Retorna null em caso de falha para que o chamador possa usar fallback local.
 */
@Slf4j
@Service
public class GroqClient implements AiClient {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GroqClient(@Value("${api.groq.api-key:}") String apiKey,
                      @Value("${api.groq.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String chat(String system, String user) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Groq API key not configured; skipping AI call");
            return null;
        }

        try {
            String body = """
                    {
                      "model": "%s",
                      "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                      ],
                      "max_tokens": 800,
                      "temperature": 0.5
                    }
                    """.formatted(model, escapeJson(system), escapeJson(user));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Groq returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            return extractContent(response.body());
        } catch (Exception e) {
            log.warn("Groq call failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractContent(String json) {
        try {
            String content = extractField(json, "\"message\":{\"content\":");
            if (content == null) {
                content = extractField(json, "\"reasoning_content\":");
            }
            return content;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractField(String json, String fieldMarker) {
        int idx = json.indexOf(fieldMarker);
        if (idx == -1) {
            return null;
        }
        int start = idx + fieldMarker.length();
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }
        start++;
        int end = json.indexOf("\"", start);
        if (end == -1) {
            return null;
        }
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\u00e9", "é")
                .replace("\\u00e1", "á")
                .replace("\\u00e3", "ã")
                .replace("\\u00e7", "ç")
                .replace("\\u00ea", "ê")
                .replace("\\u00f3", "ó")
                .replace("\\u00ed", "í")
                .replace("\\u00fa", "ú")
                .replace("\\u00f4", "ô")
                .replace("\\u00e0", "à");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
