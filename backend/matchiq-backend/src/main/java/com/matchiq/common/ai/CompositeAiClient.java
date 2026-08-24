package com.matchiq.common.ai;

import com.matchiq.recommendation.service.HuggingFaceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Tenta o Groq primeiro (rápido) e, se indisponível, cai no Hugging Face.
 * É o bean @Primary injetado onde quer que se peça um AiClient.
 */
@Slf4j
@Primary
@Service
public class CompositeAiClient implements AiClient {

    private final GroqClient groqClient;
    private final HuggingFaceClient huggingFaceClient;

    public CompositeAiClient(GroqClient groqClient, HuggingFaceClient huggingFaceClient) {
        this.groqClient = groqClient;
        this.huggingFaceClient = huggingFaceClient;
    }

    @Override
    public String chat(String system, String user) {
        String result = groqClient.chat(system, user);
        if (result != null && !result.isBlank()) {
            return result;
        }
        log.info("Groq indisponível, usando Hugging Face como fallback");
        return huggingFaceClient.chat(system, user);
    }
}
