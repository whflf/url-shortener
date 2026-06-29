package com.example.core_app.scheduler;

import com.example.core_app.model.Link;
import com.example.core_app.repository.LinkRepository;
import com.example.core_app.service.LinkDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkExpirationScheduler {
    private final LinkRepository repository;
    private final LinkDataService dataService;

    @Scheduled(cron = "${app.link.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredLinks() {
        List<Link> expired = repository.findByExpiresAtBefore(LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(link -> dataService.evictFromCache(link.getShortCode()));
        repository.deleteAll(expired);
        log.info("Purged {} expired link(s)", expired.size());
    }
}
