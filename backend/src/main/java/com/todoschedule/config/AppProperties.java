package com.todoschedule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * application.properties 中 app.* 的型別化設定。
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String apiToken,
        Cors cors,
        Resend resend,
        String frontendUrl
) {
    public record Cors(List<String> allowedOrigins) {}

    public record Resend(String apiKey, String fromEmail) {}
}
