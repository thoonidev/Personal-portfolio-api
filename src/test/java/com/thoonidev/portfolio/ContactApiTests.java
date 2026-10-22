package com.thoonidev.portfolio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import com.thoonidev.portfolio.exception.ContactUnavailableException;
import com.thoonidev.portfolio.dto.ContactRequest;
import com.thoonidev.portfolio.service.ContactMailService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thoonidev.portfolio.service.ContactRateLimiter;

@SpringBootTest(properties = {
    "app.contact.enabled=true",
    "app.contact.recipient=owner@example.test",
    "app.contact.from=portfolio@example.test", "app.mail.api-key=test-key"
})
@AutoConfigureMockMvc
class ContactApiTests {
    private static final String VALID = """
            {"name":"  Ana Pérez  ","email":"ana@example.com","subject":"Una oportunidad",
             "message":"Me gustaría conversar sobre tu trabajo.","website":""}
            """;

    @Autowired private MockMvc mvc;
    @MockitoBean private ContactMailService sender;
    @MockitoBean private ContactRateLimiter limiter;

    @Test
    void healthCheckDoesNotSendMail() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void normalizesAndPassesValidatedContactToDeliveryService() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").exists());
        ArgumentCaptor<ContactRequest> captor = ArgumentCaptor.forClass(ContactRequest.class);
        verify(sender).send(captor.capture());


        assertThat(captor.getValue().email()).isEqualTo("ana@example.com");
        assertThat(captor.getValue().name()).isEqualTo("Ana Pérez");
    }

    @Test
    void rejectsBlankAndMalformedFieldsWithoutSending() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"   ","email":"invalid","subject":"a","message":"short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.subject").exists())
                .andExpect(jsonPath("$.errors.message").exists());
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void rejectsHeaderInjection() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("Una oportunidad", "Hola\\r\\nBcc: attacker@example.com")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.subject").exists());
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void rejectsHoneypot() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("\"website\":\"\"", "\"website\":\"spam\"")))
                .andExpect(status().isBadRequest());
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void mailFailureIsNotReportedAsSuccess() throws Exception {
        doThrow(new ContactUnavailableException()).when(sender).send(any(ContactRequest.class));
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("No se pudo enviar el mensaje. Inténtalo más tarde o utiliza el correo de contacto."));
    }

    @Test
    void malformedJsonIsAClientError() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void largeBodiesAreRejectedEvenWithoutContentLength() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content("x".repeat(20_000)).with(request -> { request.removeHeader("Content-Length"); return request; }))
                .andExpect(status().is(413));
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void permitsOnlyConfiguredFrontendOrigins() throws Exception {
        mvc.perform(options("/api/contact").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(post("/api/contact").header("Origin", "https://untrusted.example")
                .contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
        verify(sender, never()).send(any(ContactRequest.class));
    }

    @Test
    void earlyRateLimitErrorsRetainCorsAndRetryHeaders() throws Exception {
        when(limiter.retryAfter(anyString())).thenReturn(60L);
        mvc.perform(post("/api/contact").header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        verify(sender, never()).send(any(ContactRequest.class));
    }
}
