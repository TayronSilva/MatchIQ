package com.matchiq.common.ai;

/**
 * Contrato comum para qualquer provedor de IA de chat.
 * Assim trocar de provider (Groq, Hugging Face, OpenAI) não exige mexer nos serviços.
 */
public interface AiClient {

    /**
     * Envia system + user e retorna o texto gerado, ou null se o provider falhar.
     */
    String chat(String system, String user);
}
