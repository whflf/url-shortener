package com.example.core_app.exception;

public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String code) {
        super("Link '" + code + "' has expired and is no longer available");
    }
}
