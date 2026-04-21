package com.example.analytics.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import com.example.analytics.model.ClickDetails;
import com.example.analytics.repository.LinkStatsRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.example.common.dto.ClickEvent;

@Service
public class AnalyticsConsumer {
    private final LinkStatsRepository repository;
    private final MeterRegistry meterRegistry;

    public AnalyticsConsumer(LinkStatsRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "link-clicks", groupId = "analytics-group")
    @Transactional
    public void consumeClick(ClickEvent clickEvent) {
        Counter.builder("link_clicks_total")
                .description("Total number of link clicks")
                .tag("browser", clickEvent.getBrowser())
                .tag("os", clickEvent.getOs())
                .tag("device", clickEvent.getDeviceType())
                .register(meterRegistry)
                .increment();

        ClickDetails stats = ClickDetails.builder()
                            .code(clickEvent.getCode())
                            .browser(clickEvent.getBrowser())
                            .os(clickEvent.getOs())
                            .deviceType(clickEvent.getDeviceType())
                            .referer(clickEvent.getReferer())
                            .timestamp(clickEvent.getTimestamp())
                            .build();
        repository.save(stats);
    }
}
