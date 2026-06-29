package com.example.core_app.service;

import com.example.core_app.exception.UnsafeUrlException;
import com.example.grpc.CheckUrlRequest;
import com.example.grpc.SafetyServiceGrpc;
import com.example.common.dto.ClickEvent;
import com.example.core_app.model.Link;
import com.example.core_app.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.debug.UserAgentAnalyzerTester;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository repository;
    private final LinkDataService dataService;
    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;

    @Value("${app.link.default-ttl-days:30}")
    private int defaultTtlDays;

    @GrpcClient("safety-service")
    private SafetyServiceGrpc.SafetyServiceBlockingStub safetyStub;

    @Transactional
    public String shortenUrl(String longUrl, Integer ttlDays) {
        int effectiveTtl = (ttlDays != null && ttlDays > 0) ? ttlDays : defaultTtlDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(effectiveTtl);

        Optional<Link> existing = repository.findByLongUrl(longUrl);
        if (existing.isPresent()) {
            Link link = existing.get();
            if (!link.isExpired()) {
                return link.getShortCode();
            }
            // Re-activate the expired link with a fresh TTL instead of violating the unique constraint
            link.setExpiresAt(expiresAt);
            link.setCreatedAt(LocalDateTime.now());
            dataService.evictFromCache(link.getShortCode());
            return repository.save(link).getShortCode();
        }

        var request = CheckUrlRequest.newBuilder().setUrl(longUrl).build();
        var response = safetyStub.checkUrl(request);
        if (!response.getIsSafe()) {
            throw new UnsafeUrlException(longUrl);
        }

        byte[] urlBytes = longUrl.getBytes(StandardCharsets.UTF_8);
        XXHashFactory factory = XXHashFactory.fastestInstance();
        XXHash64 hash64 = factory.hash64();
        long hashed = hash64.hash(urlBytes, 0, urlBytes.length, 19L);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(hashed);

        String code = Base64.getUrlEncoder().encodeToString(buffer.array());

        Link link = Link.builder()
                .longUrl(longUrl)
                .shortCode(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        repository.save(link);
        return code;
    }

    public String getOriginalUrl(String code, String userAgent, String referer) {
        String originalUrl = dataService.getUrlFromDb(code);

        var analyzer = UserAgentAnalyzerTester.newBuilder().hideMatcherLoadStats().build();
        UserAgent agent = analyzer.parse(userAgent);

        ClickEvent event = ClickEvent.builder()
                .code(code)
                .browser(agent.getValue(UserAgent.AGENT_NAME))
                .os(agent.getValue(UserAgent.OPERATING_SYSTEM_NAME))
                .deviceType(agent.getValue(UserAgent.DEVICE_CLASS))
                .referer(referer)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("link-clicks", event);
        return originalUrl;
    }
}
