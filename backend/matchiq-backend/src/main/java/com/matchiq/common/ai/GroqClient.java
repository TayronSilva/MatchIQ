package com.matchiq.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqClient(@Value("${api.groq.api-key:}") String apiKey,
                      @Value("${api.groq.model:openai/gpt-oss-20b}") String model) {
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
                      "max_tokens": 3000,
                      "temperature": 0.3
                    }
                    """.formatted(model, escapeJson(system), escapeJson(user));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText(null);
                String reasoning = message.path("reasoning_content").asText(null);
                return pickResume(content, reasoning);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String pickResume(String content, String reasoning) {
        if (isResumeLike(content)) {
            return content;
        }
        if (isResumeLike(reasoning)) {
            return reasoning;
        }
        String recovered = extractResumeFromReasoning(content);
        if (recovered != null) {
            return recovered;
        }
        recovered = extractResumeFromReasoning(reasoning);
        if (recovered != null) {
            return recovered;
        }
        if (content != null && !content.isBlank()) {
            return content;
        }
        return reasoning;
    }

    private String extractResumeFromReasoning(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        java.util.regex.Matcher m = H1_PATTERN.matcher(text);
        if (m.find()) {
            String slice = text.substring(m.start());
            if (slice.contains("## ")) {
                return slice;
            }
        }
        return null;
    }

    private static final java.util.regex.Pattern H1_PATTERN =
            java.util.regex.Pattern.compile("(?m)^# .*");

    /**
     * Um currículo em Markdown válido começa com H1 ("# Nome") e traz seções "## ".
     * O pensamento do modelo (reasoning) é prosa solta e não atende a esses critérios.
     */
    private boolean isResumeLike(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String t = text.trim();
        return t.startsWith("#") && t.contains("## ")
                && !t.toLowerCase().contains("o usuário")
                && !t.toLowerCase().contains("vou verificar")
                && !t.toLowerCase().contains("preciso ");
    }

    private String decode(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'n') { sb.append('\n'); i += 2; continue; }
                if (n == 't') { sb.append('\t'); i += 2; continue; }
                if (n == 'r') { sb.append('\r'); i += 2; continue; }
                if (n == '"') { sb.append('"'); i += 2; continue; }
                if (n == '\\') { sb.append('\\'); i += 2; continue; }
                if (n == 'u' && i + 5 < s.length()) {
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                        i += 6;
                        continue;
                    } catch (NumberFormatException ignore) { }
                }
                sb.append(n);
                i += 2;
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
