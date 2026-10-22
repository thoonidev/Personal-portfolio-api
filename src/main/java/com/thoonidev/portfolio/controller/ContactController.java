package com.thoonidev.portfolio.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.thoonidev.portfolio.dto.ContactRequest;
import com.thoonidev.portfolio.dto.ContactResponse;
import com.thoonidev.portfolio.service.ContactService;

import jakarta.validation.Valid;

@RestController
public class ContactController {
    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/contact", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ContactResponse submit(@Valid @RequestBody ContactRequest contact) {
        service.submit(contact);
        return new ContactResponse("Tu mensaje fue aceptado por el servicio de correo. Gracias por escribirme.");
    }
}
