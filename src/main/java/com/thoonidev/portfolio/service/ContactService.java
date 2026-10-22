package com.thoonidev.portfolio.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.thoonidev.portfolio.dto.ContactRequest;

@Service
public class ContactService {
    private final ContactMailService mailService;

    public ContactService(ContactMailService mailService) {
        this.mailService = mailService;
    }

    public void submit(ContactRequest contact) {
        if (contact.website() != null && !contact.website().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo procesar el formulario.");
        }
        mailService.send(contact);
    }
}
