package com.thoonidev.portfolio.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.contact")
public record ContactProperties(
        boolean enabled,
        String recipient,
        String from,
        List<String> allowedOrigins,
        int maxRequests,
        int globalMaxRequests,
        Duration window) {

    public ContactProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()
                || allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("Configura orígenes explícitos para el formulario.");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
        if (maxRequests < 1 || globalMaxRequests < 1 || window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Los límites de contacto deben ser positivos.");
        }
        if (enabled && (!validAddress(recipient) || !validAddress(from))) {
            throw new IllegalArgumentException("Configura CONTACT_TO y CONTACT_FROM antes de habilitar el correo.");
        }
    }

    private static boolean validAddress(String address) {
        return address != null && address.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+");
    }
}
