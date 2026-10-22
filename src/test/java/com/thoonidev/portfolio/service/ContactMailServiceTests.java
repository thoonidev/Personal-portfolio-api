package com.thoonidev.portfolio.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.thoonidev.portfolio.config.ContactProperties;
import com.thoonidev.portfolio.config.MailApiProperties;
import com.thoonidev.portfolio.dto.ContactRequest;
import com.thoonidev.portfolio.exception.ContactUnavailableException;

class ContactMailServiceTests {
    private MockRestServiceServer server;
    private RestClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.resend.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
    }

    @Test
    void sendsFixedAddressesAndVisitorReplyToViaHttps() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(content().json("""
                    {"from":"portfolio@example.test","to":["owner@example.test"],
                    "reply_to":"ana@example.com","subject":"[Portfolio] Una oportunidad"}
                    """))
                .andRespond(withSuccess("{\"id\":\"test-delivery-id\"}", MediaType.APPLICATION_JSON));
        service(true).send(request());
        server.verify();
    }

    @Test
    void disabledDeliveryDoesNotCallProvider() {
        assertThatThrownBy(() -> service(false).send(request())).isInstanceOf(ContactUnavailableException.class);
        server.verify();
    }

    @Test
    void providerRejectionDoesNotReportSuccess() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("private provider details"));
        assertThatThrownBy(() -> service(true).send(request())).isInstanceOf(ContactUnavailableException.class)
                .hasMessageNotContaining("private provider details");
        server.verify();
    }

    @Test
    void successfulHttpWithoutDeliveryIdDoesNotReportSuccess() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service(true).send(request())).isInstanceOf(ContactUnavailableException.class);
        server.verify();
    }

    @Test
    void providerServerFailureDoesNotReportSuccess() {
        server.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());
        assertThatThrownBy(() -> service(true).send(request())).isInstanceOf(ContactUnavailableException.class);
        server.verify();
    }

    private ContactMailService service(boolean enabled) {
        ContactProperties properties = new ContactProperties(enabled, "owner@example.test", "portfolio@example.test",
                List.of("http://localhost:5173"), 5, 50, Duration.ofMinutes(10));
        return new ContactMailService(client, properties, new MailApiProperties("test-key"));
    }

    private ContactRequest request() {
        return new ContactRequest("Ana", "ana@example.com", "Una oportunidad", "Me gustaría conversar sobre tu trabajo.", "");
    }
}
