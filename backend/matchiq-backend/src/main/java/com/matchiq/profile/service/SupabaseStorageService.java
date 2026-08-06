package com.matchiq.profile.service;

import com.matchiq.config.SupabaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final SupabaseProperties properties;
    private final RestClient.Builder restClientBuilder;

    public String uploadAvatar(MultipartFile file) {
        String fileName = "avatars/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());

        RestClient restClient = restClientBuilder.build();

        restClient.post()
                .uri(properties.getUrl() + "/storage/v1/object/" + properties.getBucket() + "/" + fileName)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .headers(headers -> headers.setBearerAuth(properties.getServiceKey()))
                .body(file.getBytes())
                .retrieve()
                .toBodilessEntity();

        return properties.getUrl() + "/storage/v1/object/public/" + properties.getBucket() + "/" + fileName;
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "avatar";
        }
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
