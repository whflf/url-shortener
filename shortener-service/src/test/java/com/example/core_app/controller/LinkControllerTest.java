package com.example.core_app.controller;

import com.example.core_app.config.RateLimitInterceptor;
import com.example.core_app.exception.LinkExpiredException;
import com.example.core_app.exception.UnsafeUrlException;
import com.example.core_app.service.LinkService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LinkController.class)
@TestPropertySource(properties = "spring.cache.type=none")
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinkService linkService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockitoBean
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shorten_returns200_withShortUrl() throws Exception {
        when(linkService.shortenUrl(eq("https://example.com"), any())).thenReturn("abc123==");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123=="));
    }

    @Test
    void shorten_returns200_withCustomTtl() throws Exception {
        when(linkService.shortenUrl(eq("https://example.com"), eq(7))).thenReturn("abc123==");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"ttlDays\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").exists());
    }

    @Test
    void shorten_returns400_whenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shorten_returns400_whenUrlIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shorten_returns409_whenUrlIsUnsafe() throws Exception {
        when(linkService.shortenUrl(any(), any()))
                .thenThrow(new UnsafeUrlException("https://phishing.example.com"));

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://phishing.example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("UNSAFE_URL"));
    }

    @Test
    void redirect_returns302_withLocationHeader() throws Exception {
        when(linkService.getOriginalUrl(eq("abc123=="), any(), any()))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/abc123==")
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void redirect_returns404_whenCodeNotFound() throws Exception {
        when(linkService.getOriginalUrl(eq("missing"), any(), any()))
                .thenThrow(new EntityNotFoundException("Link not found for code: missing"));

        mockMvc.perform(get("/missing")
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("LINK_NOT_FOUND"));
    }

    @Test
    void redirect_returns410_whenLinkIsExpired() throws Exception {
        when(linkService.getOriginalUrl(eq("old123"), any(), any()))
                .thenThrow(new LinkExpiredException("old123"));

        mockMvc.perform(get("/old123")
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.errorCode").value("LINK_EXPIRED"));
    }
}
