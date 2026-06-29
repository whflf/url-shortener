package com.example.core_app;

import com.example.core_app.repository.LinkRepository;
import com.example.grpc.SafetyServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class UrlShortenerApplicationTests {

    @MockitoBean
    LinkRepository linkRepository;

    @MockitoBean
    KafkaTemplate<?, ?> kafkaTemplate;

    @MockitoBean
    SafetyServiceGrpc.SafetyServiceBlockingStub safetyStub;

    @Test
    void contextLoads() {
    }
}
