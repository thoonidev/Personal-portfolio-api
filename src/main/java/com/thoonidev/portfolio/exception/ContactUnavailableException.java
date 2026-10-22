package com.thoonidev.portfolio.exception;

public class ContactUnavailableException extends RuntimeException {
    public ContactUnavailableException() {
        super("No se pudo enviar el mensaje. Inténtalo más tarde o utiliza el correo de contacto.");
    }
}
