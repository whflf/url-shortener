package com.example.core_app.service;

import com.example.core_app.exception.LinkExpiredException;
import com.example.core_app.model.Link;
import com.example.core_app.repository.LinkRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkDataServiceTest {

    @Mock
    private LinkRepository repository;

    @InjectMocks
    private LinkDataService dataService;

    @Test
    void getUrlFromDb_returnsLongUrl_whenLinkExistsAndIsActive() {
        Link link = Link.builder()
                .shortCode("abc123")
                .longUrl("https://example.com")
                .expiresAt(LocalDateTime.now().plusDays(10))
                .build();
        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(link));

        String result = dataService.getUrlFromDb("abc123");

        assertThat(result).isEqualTo("https://example.com");
    }

    @Test
    void getUrlFromDb_throwsEntityNotFoundException_whenCodeDoesNotExist() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataService.getUrlFromDb("missing"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getUrlFromDb_throwsLinkExpiredException_whenLinkIsExpired() {
        Link link = Link.builder()
                .shortCode("old123")
                .longUrl("https://example.com/old")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(repository.findByShortCode("old123")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> dataService.getUrlFromDb("old123"))
                .isInstanceOf(LinkExpiredException.class)
                .hasMessageContaining("old123");
    }

    @Test
    void getUrlFromDb_returnsUrl_whenLinkHasNoExpiry() {
        Link link = Link.builder()
                .shortCode("noexp")
                .longUrl("https://example.com/permanent")
                .expiresAt(null)
                .build();
        when(repository.findByShortCode("noexp")).thenReturn(Optional.of(link));

        String result = dataService.getUrlFromDb("noexp");

        assertThat(result).isEqualTo("https://example.com/permanent");
    }
}
