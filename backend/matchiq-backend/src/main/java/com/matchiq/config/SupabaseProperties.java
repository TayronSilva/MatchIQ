package com.matchiq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url;
    private String serviceKey;
    private String bucket = "avatars";
}
