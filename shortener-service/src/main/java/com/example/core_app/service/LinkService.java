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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository repository;
    private final LinkDataService dataService;
    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;

    @GrpcClient("safety-service")
    private SafetyServiceGrpc.SafetyServiceBlockingStub safetyStub;

    @Transactional
    public String shortenUrl(String longUrl) {
        if (repository.findByLongUrl(longUrl).isPresent()) {
            return repository.findByLongUrl(longUrl).get().getShortCode();
        }

        var request = CheckUrlRequest.newBuilder().setUrl(longUrl).build();
        var response = safetyStub.checkUrl(request);

        if (!response.getIsSafe()) {
            throw new UnsafeUrlException(longUrl);
        }

        byte[] urlBytes = longUrl.getBytes(StandardCharsets.UTF_8);

        XXHashFactory factory = XXHashFactory.fastestInstance();
        XXHash64 hash64 = factory.hash64();

        long seed = 19;
        long hashed = hash64.hash(urlBytes, 0, urlBytes.length, seed);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(hashed);
        byte[] hashedBytes = buffer.array();

        Base64.Encoder encoder = Base64.getUrlEncoder();

        String code = encoder.encodeToString(hashedBytes);

        Link link = Link.builder()
                .longUrl(longUrl)
                .shortCode(code)
                .createdAt(LocalDateTime.now())
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
