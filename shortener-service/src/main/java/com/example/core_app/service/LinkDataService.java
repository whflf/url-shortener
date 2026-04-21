package com.example.core_app.service;

import com.example.core_app.model.Link;
import com.example.core_app.repository.LinkRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkDataService {
    private final LinkRepository repository;

    @Cacheable(value = "links", key = "#code")
    public String getUrlFromDb(String code) {
        return repository.findByShortCode(code)
                .map(Link::getLongUrl)
                .orElseThrow(() -> new EntityNotFoundException("Link not found"));
    }
}
