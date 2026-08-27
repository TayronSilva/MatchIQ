package com.matchiq.recommendation.service;

import com.matchiq.common.ai.AiClient;
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
 * Cliente para o router de Inference Providers da Hugging Face (free tier),
 * endpoint compatível com a OpenAI API: POST /v1/chat/completions.
 * Retorna null quando a chamada falha (rede, token inválido, modelo indisponível, etc.)
 * para que o serviço caia no fallback local.
 *
 * Funciona como fallback do Groq via {@link com.matchiq.common.ai.CompositeAiClient}.
 */
@Slf4j
@Service
public class HuggingFaceClient implements AiClient {

    private static final String API_URL = "https://router.huggingface.co/v1/chat/completions";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public HuggingFaceClient(@Value("${api.huggingface.api-key:}") String apiKey,
                             @Value("${api.huggingface.model:}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Implementação do contrato AiClient: combina system + user num único prompt,
     * já que o free router da Hugging Face não separa papéis de forma confiável.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String system, String user) {
        String prompt = (system == null || system.isBlank()) ? user : (system + "\n\n" + user);
        return generate(prompt);
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
                    {
                      "model": "%s",
                      "messages": [{"role": "user", "content": "%s"}],
                      "max_tokens": 3000,
                      "temperature": 0.4
                    }
                    """.formatted(model, escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.warn("HuggingFace returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            return extractContent(response.body());
        } catch (Exception e) {
            log.warn("HuggingFace call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o texto de choices[0].message.content (ou reasoning_content como fallback)
     * da resposta JSON do chat completions.
     *
     * Usa um parser JSON real (Jackson) em vez de casar substrings, porque a resposta
     * OpenAI-compatible traz "role" antes de "content" (ex.: {"message":{"role":"assistant",
     * "content":"..."}}). O casamento rígido anterior falhava e acabava retornando o
     * reasoning_content (o pensamento interno do modelo) como se fosse a resposta — o que
     * vazava o raciocínio da IA no currículo gerado.
     *
     * Como modelos de raciocínio (DeepSeek) às vezes devolvem o pensamento em "content" e a
     * resposta em "reasoning_content" (ou vice-versa), escolhemos o campo que PARECE o
     * currículo: começar com H1 ("# Nome") e conter seções "## ".
     */
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
        // Modelos de raciocínio às vezes devolvem SÓ o pensamento em "content" (sem
        // resposta separada). O currículo costuma aparecer ao final do raciocínio, iniciando
        // com "# Nome". Recuperamos a partir do primeiro cabeçalho H1.
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

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
