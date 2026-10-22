package com.thoonidev.portfolio.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.thoonidev.portfolio.config.ContactProperties;
import com.thoonidev.portfolio.config.MailApiProperties;
import com.thoonidev.portfolio.dto.ContactRequest;
import com.thoonidev.portfolio.exception.ContactUnavailableException;

@Service
public class ContactMailService {
    private static final Logger log = LoggerFactory.getLogger(ContactMailService.class);
    private final RestClient client;
    private final ContactProperties contact;
    private final MailApiProperties mail;

    public ContactMailService(RestClient client, ContactProperties contact, MailApiProperties mail) {
        this.client = client;
        this.contact = contact;
        this.mail = mail;
    }

    public void send(ContactRequest request) {
        if (!contact.enabled()) {
            log.warn("Envío deshabilitado: configura CONTACT_ENABLED=true en la ejecución de Spring Boot y reinicia la aplicación.");
            throw new ContactUnavailableException();
        }
        if (mail.apiKey() == null || mail.apiKey().isBlank()) {
            log.warn("No hay una clave API de correo configurada en la ejecución de Spring Boot.");
            throw new ContactUnavailableException();
        }
        Map<String, Object> payload = Map.of(
                "from", contact.from(),
                "to", List.of(contact.recipient()),
                "reply_to", request.email(),
                "subject", "[Portfolio] " + request.subject(),
                "text", "Nombre: %s%nCorreo: %s%n%n%s".formatted(
                        request.name(), request.email(), request.message()));
        try {
            DeliveryResponse response = client.post().uri("/emails")
                    .headers(headers -> headers.setBearerAuth(mail.apiKey()))
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().body(DeliveryResponse.class);
            if (response == null || response.id() == null || response.id().isBlank()) {
                log.warn("Resend respondió sin un identificador de envío; no se confirmó el mensaje.");
                throw new ContactUnavailableException();
            }
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String hint = switch (status) {
                case 400, 422 -> "Revisa el remitente autorizado y la configuración del correo en Resend.";
                case 401 -> "Revisa que RESEND_API_KEY sea válida, esté activa y pertenezca a tu cuenta.";
                case 403 -> "Revisa los permisos de la clave, el dominio del remitente y el destinatario permitido para pruebas.";
                case 429 -> "El proveedor está limitando las solicitudes o se alcanzó la cuota de envío.";
                default -> "Comprueba el estado y los registros del proveedor de correo.";
            };
            // No imprimir el cuerpo de la respuesta: puede contener direcciones privadas.
            log.warn("Resend rechazó la petición: HTTP {}. {}", status, hint);
            throw new ContactUnavailableException();
        } catch (RestClientException exception) {
            // Nunca registrar la clave, la respuesta privada ni el mensaje del visitante.
            log.warn("No se pudo confirmar la conexión o respuesta de Resend ({}). Revisa la conectividad HTTPS y el tiempo de espera.", exception.getClass().getSimpleName());
            throw new ContactUnavailableException();
        }
    }

    public record DeliveryResponse(String id) { }
}
