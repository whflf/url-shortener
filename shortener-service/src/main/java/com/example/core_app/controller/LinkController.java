package com.example.core_app.controller;

import com.example.core_app.dto.ShortenRequest;
import com.example.core_app.dto.ShortenResponse;
import com.example.core_app.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class LinkController {
    private final LinkService service;

    @PostMapping("/api/v1/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String code = service.shortenUrl(request.url(), request.ttlDays());
        return ResponseEntity.ok(new ShortenResponse("http://localhost:8080/" + code));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code,
                                         @RequestHeader("User-Agent") String userAgent,
                                         @RequestHeader(value = "Referer", required = false) String referer) {
        String originalUrl = service.getOriginalUrl(code, userAgent, referer);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}
