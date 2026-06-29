package com.example.core_app.service;

import com.example.common.dto.ClickEvent;
import com.example.core_app.exception.UnsafeUrlException;
import com.example.core_app.model.Link;
import com.example.core_app.repository.LinkRepository;
import com.example.grpc.CheckUrlRequest;
import com.example.grpc.CheckUrlResponse;
import com.example.grpc.SafetyServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock private LinkRepository repository;
    @Mock private LinkDataService dataService;
    @Mock private KafkaTemplate<String, ClickEvent> kafkaTemplate;
    @Mock private SafetyServiceGrpc.SafetyServiceBlockingStub safetyStub;

    @InjectMocks
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkService, "safetyStub", safetyStub);
        ReflectionTestUtils.setField(linkService, "defaultTtlDays", 30);
    }

    @Test
    void shortenUrl_returnsExistingCode_whenActiveUrlAlreadyPresent() {
        Link existing = Link.builder()
                .longUrl("https://example.com")
                .shortCode("abc123==")
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();
        when(repository.findByLongUrl("https://example.com")).thenReturn(Optional.of(existing));

        String code = linkService.shortenUrl("https://example.com", null);

        assertThat(code).isEqualTo("abc123==");
        verifyNoInteractions(safetyStub);
        verify(repository, never()).save(any());
    }

    @Test
    void shortenUrl_reactivatesExpiredLink_withFreshTtl() {
        Link expired = Link.builder()
                .longUrl("https://example.com")
                .shortCode("abc123==")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(repository.findByLongUrl("https://example.com")).thenReturn(Optional.of(expired));
        when(repository.save(any())).thenReturn(expired);

        linkService.shortenUrl("https://example.com", 7);

        verify(repository).save(expired);
        assertThat(expired.getExpiresAt()).isAfter(LocalDateTime.now());
        verifyNoInteractions(safetyStub);
    }

    @Test
    void shortenUrl_throwsUnsafeUrlException_whenSafetyCheckFails() {
        when(repository.findByLongUrl(any())).thenReturn(Optional.empty());
        when(safetyStub.checkUrl(any(CheckUrlRequest.class)))
                .thenReturn(CheckUrlResponse.newBuilder().setIsSafe(false).build());

        assertThatThrownBy(() -> linkService.shortenUrl("https://phishing.example.com", null))
                .isInstanceOf(UnsafeUrlException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shortenUrl_savesNewLink_andReturnsCode_whenUrlIsNew() {
        when(repository.findByLongUrl(any())).thenReturn(Optional.empty());
        when(safetyStub.checkUrl(any(CheckUrlRequest.class)))
                .thenReturn(CheckUrlResponse.newBuilder().setIsSafe(true).build());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String code = linkService.shortenUrl("https://example.com/new", null);

        assertThat(code).isNotBlank();
        ArgumentCaptor<Link> captor = ArgumentCaptor.forClass(Link.class);
        verify(repository).save(captor.capture());
        Link saved = captor.getValue();
        assertThat(saved.getLongUrl()).isEqualTo("https://example.com/new");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void shortenUrl_usesTtlDaysParam_whenProvided() {
        when(repository.findByLongUrl(any())).thenReturn(Optional.empty());
        when(safetyStub.checkUrl(any(CheckUrlRequest.class)))
                .thenReturn(CheckUrlResponse.newBuilder().setIsSafe(true).build());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        linkService.shortenUrl("https://example.com/ttl", 7);

        ArgumentCaptor<Link> captor = ArgumentCaptor.forClass(Link.class);
        verify(repository).save(captor.capture());
        LocalDateTime expiresAt = captor.getValue().getExpiresAt();
        assertThat(expiresAt).isBetween(
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(8));
    }

    @Test
    void getOriginalUrl_returnsUrl_andPublishesClickEvent() {
        when(dataService.getUrlFromDb("abc123")).thenReturn("https://example.com");

        String url = linkService.getOriginalUrl("abc123", "Mozilla/5.0", "https://referrer.com");

        assertThat(url).isEqualTo("https://example.com");
        verify(kafkaTemplate).send(eq("link-clicks"), any(ClickEvent.class));
    }
}
