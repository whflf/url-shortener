package com.example.core_app.exception;

import lombok.Data;

@Data
public class UnsafeUrlException extends RuntimeException {
    private final String originalUrl;

    public UnsafeUrlException(String originalUrl) {
        super(String.format("URL %s is unsafe", originalUrl));
        this.originalUrl = originalUrl;
    }
}
