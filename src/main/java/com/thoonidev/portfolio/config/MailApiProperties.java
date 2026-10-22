package com.thoonidev.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailApiProperties(String apiKey) {
    @Override
    public String toString() { return "MailApiProperties[apiKey=REDACTED]"; }
}
