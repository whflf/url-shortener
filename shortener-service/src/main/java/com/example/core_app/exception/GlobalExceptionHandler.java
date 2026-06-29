package com.example.core_app.exception;

import com.example.core_app.dto.ErrorDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsafeUrlException.class)
    public ResponseEntity<ErrorDetails> handleUnsafeUrl(UnsafeUrlException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, "UNSAFE_URL");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleLinkNotFound(EntityNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, "LINK_NOT_FOUND");
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ResponseEntity<ErrorDetails> handleLinkExpired(LinkExpiredException ex, WebRequest request) {
        return build(HttpStatus.GONE, ex.getMessage(), request, "LINK_EXPIRED");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request, "VALIDATION_ERROR");
    }

    private ResponseEntity<ErrorDetails> build(HttpStatus status, String message, WebRequest request, String code) {
        return new ResponseEntity<>(
                new ErrorDetails(LocalDateTime.now(), message, request.getDescription(false), code),
                status);
    }
}
