package com.matchiq.recommendation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente para a Serverless Inference API da Hugging Face (free tier).
 * Retorna null quando a chamada falha (rede, token inválido, modelo carregando, etc.)
 * para que o serviço caia no fallback local.
 */
@Slf4j
@Service
public class HuggingFaceClient {

    private static final String API_URL = "https://api-inference.huggingface.co/models/";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public HuggingFaceClient(@Value("${api.huggingface.api-key:}") String apiKey,
                             @Value("${api.huggingface.model:}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Envia um prompt ao modelo e retorna o texto gerado, ou null em caso de falha.
     */
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("HuggingFace API key not configured; skipping AI call");
            return null;
        }

        try {
            String body = """
                    {"inputs": "%s", "parameters": {"max_new_tokens": 500, "temperature": 0.7}}
                    """.formatted(escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + model))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("HuggingFace returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            // a resposta vem como um array JSON: [{"generated_text": "..."}]
            return extractGeneratedText(response.body());
        } catch (Exception e) {
            log.warn("HuggingFace call failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractGeneratedText(String json) {
        try {
            int idx = json.indexOf("\"generated_text\":\"");
            if (idx == -1) {
                return null;
            }
            int start = idx + "\"generated_text\":\"".length();
            int end = json.indexOf("\"", start);
            if (end == -1) {
                return null;
            }
            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\u003c", "<")
                    .replace("\\u003e", ">");
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
