package com.thoonidev.portfolio.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MailApiProperties.class)
public class MailApiConfig {
    @Bean
    public RestClient mailClient(ContactProperties contact, MailApiProperties mail) {
        if (contact.enabled() && (mail.apiKey() == null || mail.apiKey().isBlank())) {
            throw new IllegalArgumentException("Configura RESEND_API_KEY antes de habilitar el envío.");
        }
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl("https://api.resend.com")
                .requestFactory(factory).build();
    }
}
