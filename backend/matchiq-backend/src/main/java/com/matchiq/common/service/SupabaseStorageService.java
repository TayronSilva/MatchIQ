package com.matchiq.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Upload de arquivos para o Supabase Storage via API REST.
 * Usa a service key (apenas no backend, nunca exposta ao cliente).
 */
@Slf4j
@Service
public class SupabaseStorageService {

    private final HttpClient httpClient;
    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;

    public SupabaseStorageService(@Value("${supabase.url:}") String supabaseUrl,
                                  @Value("${supabase.service-key:}") String serviceKey,
                                  @Value("${supabase.bucket:avatars}") String bucket) {
        this.supabaseUrl = supabaseUrl;
        this.serviceKey = serviceKey;
        this.bucket = bucket;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Envia o arquivo para o bucket e retorna a URL pública.
     * Ex.: https://<projeto>.supabase.co/storage/v1/object/public/avatars/<uuid>.png
     */
    public String upload(MultipartFile file) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("Supabase storage is not configured");
        }

        try {
            String extension = extensionOf(file.getOriginalFilename());
            String objectName = UUID.randomUUID() + extension;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectName))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                log.warn("Supabase upload failed: {} {}", response.statusCode(), response.body());
                throw new IllegalStateException("Falha ao enviar arquivo para o storage");
            }

            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectName;
        } catch (Exception e) {
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            log.warn("Supabase upload error: {}", e.getMessage());
            throw new IllegalStateException("Falha ao enviar arquivo para o storage");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? "" : filename.substring(dot).toLowerCase();
    }
}
